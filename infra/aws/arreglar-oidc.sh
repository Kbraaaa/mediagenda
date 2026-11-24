#!/bin/bash
# Nota: desde 2026 GitHub identifica el repositorio con IDs fijos en el claim "sub":
# repo:Kbraaaa@134634912/mediagenda@1469382444:ref:refs/heads/main (formato nuevo). La regla acepta ese y el antiguo.
# Diagnostica y corrige la entrada de GitHub Actions a AWS (OIDC). Se corre en AWS CloudShell.
CUENTA=$(aws sts get-caller-identity --query Account --output text)
PROVEEDOR=arn:aws:iam::$CUENTA:oidc-provider/token.actions.githubusercontent.com
echo "== 1. Proveedor OIDC de GitHub"
if aws iam get-open-id-connect-provider --open-id-connect-provider-arn $PROVEEDOR --query ClientIDList; then
  echo "   existe"
else
  echo "   NO existe: se crea"
  aws iam create-open-id-connect-provider --url https://token.actions.githubusercontent.com --client-id-list sts.amazonaws.com
fi
echo "== 2. Regla de confianza del rol: cualquier trabajo de Kbraaaa/mediagenda (rama main o entorno aws)"
cat > /tmp/confianza.json <<JSON
{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Federated":"$PROVEEDOR"},
 "Action":"sts:AssumeRoleWithWebIdentity","Condition":{
  "StringEquals":{"token.actions.githubusercontent.com:aud":"sts.amazonaws.com"},
  "StringLike":{"token.actions.githubusercontent.com:sub":["repo:Kbraaaa@134634912/mediagenda@1469382444:*","repo:Kbraaaa/mediagenda:*"]}}}]}
JSON
aws iam update-assume-role-policy --role-name mediagenda-github-actions --policy-document file:///tmp/confianza.json &&
  echo "   regla actualizada"
echo "== 3. Comprobacion"
aws iam list-open-id-connect-providers --output text
aws iam get-role --role-name mediagenda-github-actions --query 'Role.AssumeRolePolicyDocument.Statement[0].Condition' --output json
