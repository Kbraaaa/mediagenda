#!/bin/bash
# Nota: desde 2026 GitHub identifica el repositorio con IDs fijos en el claim "sub":
# repo:Kbraaaa@134634912/mediagenda@1469382444:ref:refs/heads/main (formato nuevo). La regla acepta ese y el antiguo.
# Configura AWS para el despliegue automatico desde GitHub Actions. Se corre UNA vez en AWS CloudShell.
# Crea: 4 repositorios de imagenes en ECR, el proveedor OIDC de GitHub, el rol que GitHub puede asumir
# (solo desde Kbraaaa/mediagenda, rama main) y le da a la EC2 permiso para descargar de ECR y recibir
# ordenes de AWS Systems Manager. Si algo ya existe, lo deja como esta.
set -u
REGION=us-east-2
REPO_GITHUB=Kbraaaa/mediagenda
ROL_EC2=mediagenda-ec2
ROL_GITHUB=mediagenda-github-actions
CUENTA=$(aws sts get-caller-identity --query Account --output text)
echo "Cuenta: $CUENTA  Region: $REGION"

echo "1/5 Repositorios de imagenes en ECR"
for s in ms-pacientes ms-citas ms-farmacia ms-notificaciones; do
  aws ecr describe-repositories --region $REGION --repository-names mediagenda/$s >/dev/null 2>&1 ||
    aws ecr create-repository --region $REGION --repository-name mediagenda/$s \
      --image-scanning-configuration scanOnPush=true --query repository.repositoryUri --output text
  # Guarda solo las ultimas 5 imagenes de cada servicio (cuidar el espacio y los creditos)
  aws ecr put-lifecycle-policy --region $REGION --repository-name mediagenda/$s --lifecycle-policy-text \
    '{"rules":[{"rulePriority":1,"description":"ultimas 5","selection":{"tagStatus":"any","countType":"imageCountMoreThan","countNumber":5},"action":{"type":"expire"}}]}' >/dev/null
done

echo "2/5 Proveedor OIDC de GitHub (GitHub entra a AWS sin claves guardadas)"
PROVEEDOR=arn:aws:iam::$CUENTA:oidc-provider/token.actions.githubusercontent.com
aws iam get-open-id-connect-provider --open-id-connect-provider-arn $PROVEEDOR >/dev/null 2>&1 ||
  aws iam create-open-id-connect-provider --url https://token.actions.githubusercontent.com \
    --client-id-list sts.amazonaws.com --query OpenIDConnectProviderArn --output text

echo "3/5 Rol para GitHub Actions"
cat > /tmp/confianza.json <<JSON
{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Federated":"$PROVEEDOR"},
 "Action":"sts:AssumeRoleWithWebIdentity","Condition":{
  "StringEquals":{"token.actions.githubusercontent.com:aud":"sts.amazonaws.com"},
  "StringLike":{"token.actions.githubusercontent.com:sub":["repo:Kbraaaa@134634912/mediagenda@1469382444:*","repo:$REPO_GITHUB:*"]}}}]}
JSON
aws iam get-role --role-name $ROL_GITHUB >/dev/null 2>&1 ||
  aws iam create-role --role-name $ROL_GITHUB --assume-role-policy-document file:///tmp/confianza.json >/dev/null
INSTANCIA=$(aws ec2 describe-instances --region $REGION --filters Name=tag:Name,Values=mediagenda \
  Name=instance-state-name,Values=running --query 'Reservations[0].Instances[0].InstanceId' --output text)
cat > /tmp/permisos.json <<JSON
{"Version":"2012-10-17","Statement":[
 {"Effect":"Allow","Action":"ecr:GetAuthorizationToken","Resource":"*"},
 {"Effect":"Allow","Action":["ecr:BatchCheckLayerAvailability","ecr:InitiateLayerUpload","ecr:UploadLayerPart",
   "ecr:CompleteLayerUpload","ecr:PutImage","ecr:BatchGetImage","ecr:GetDownloadUrlForLayer"],
  "Resource":"arn:aws:ecr:$REGION:$CUENTA:repository/mediagenda/*"},
 {"Effect":"Allow","Action":"ssm:SendCommand","Resource":[
   "arn:aws:ec2:$REGION:$CUENTA:instance/$INSTANCIA","arn:aws:ssm:$REGION::document/AWS-RunShellScript"]},
 {"Effect":"Allow","Action":["ssm:GetCommandInvocation","ssm:ListCommandInvocations"],"Resource":"*"}]}
JSON
aws iam put-role-policy --role-name $ROL_GITHUB --policy-name mediagenda-despliegue --policy-document file:///tmp/permisos.json

echo "4/5 Permisos extra para la EC2: descargar de ECR y recibir ordenes de Systems Manager"
aws iam attach-role-policy --role-name $ROL_EC2 --policy-arn arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly
aws iam attach-role-policy --role-name $ROL_EC2 --policy-arn arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore

echo "5/5 Listo. Valores para GitHub (Settings > Secrets and variables > Actions > Variables):"
echo "AWS_ROLE_ARN=arn:aws:iam::$CUENTA:role/$ROL_GITHUB"
echo "EC2_INSTANCE_ID=$INSTANCIA"
echo "ECR_REGISTRO=$CUENTA.dkr.ecr.$REGION.amazonaws.com/mediagenda"
