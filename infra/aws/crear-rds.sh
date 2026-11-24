#!/bin/bash
# Crea la base de datos administrada de MediAgenda en Amazon RDS. Se corre UNA vez en AWS CloudShell.
# - PostgreSQL 17, db.t4g.micro, 20 GB gp3 cifrados, copia de seguridad diaria (1 dia), sin acceso publico.
# - Solo la EC2 de MediAgenda puede conectarse (grupo de seguridad propio, puerto 5432).
# - La clave es la misma DB_PASSWORD del .env del servidor (se pide oculta, no se guarda aqui).
set -e
REGION=us-east-2
ID=mediagenda-db
read -s -p "Pega la DB_PASSWORD de mediagenda-servidor.env (no se vera): " CLAVE; echo

INSTANCIA=$(aws ec2 describe-instances --region $REGION --filters Name=tag:Name,Values=mediagenda \
  Name=instance-state-name,Values=running --query 'Reservations[0].Instances[0]' --output json)
VPC=$(echo "$INSTANCIA" | python3 -c "import json,sys;print(json.load(sys.stdin)['VpcId'])")
SG_EC2=$(echo "$INSTANCIA" | python3 -c "import json,sys;print(json.load(sys.stdin)['SecurityGroups'][0]['GroupId'])")
echo "VPC: $VPC  Grupo de seguridad de la EC2: $SG_EC2"

echo "1/3 Grupo de seguridad de la base: solo la EC2 entra al puerto 5432"
SG_RDS=$(aws ec2 describe-security-groups --region $REGION --filters Name=group-name,Values=mediagenda-rds \
  Name=vpc-id,Values=$VPC --query 'SecurityGroups[0].GroupId' --output text)
if [ "$SG_RDS" = "None" ]; then
  SG_RDS=$(aws ec2 create-security-group --region $REGION --group-name mediagenda-rds --vpc-id $VPC \
    --description "MediAgenda RDS: solo desde la EC2" --query GroupId --output text)
  aws ec2 authorize-security-group-ingress --region $REGION --group-id $SG_RDS \
    --protocol tcp --port 5432 --source-group $SG_EC2 >/dev/null
fi
echo "   $SG_RDS"

echo "2/3 Creando la instancia RDS (tarda 5 a 10 minutos)..."
if ! aws rds describe-db-instances --region $REGION --db-instance-identifier $ID >/dev/null 2>&1; then
  aws rds create-db-instance --region $REGION --db-instance-identifier $ID \
    --engine postgres --db-instance-class db.t4g.micro \
    --allocated-storage 20 --storage-type gp3 --storage-encrypted \
    --master-username postgres --master-user-password "$CLAVE" \
    --vpc-security-group-ids $SG_RDS --no-publicly-accessible --no-multi-az \
    --backup-retention-period 1 --no-deletion-protection \
    --tags Key=Name,Value=mediagenda --query 'DBInstance.[DBInstanceIdentifier,EngineVersion,DBInstanceStatus]' --output text
fi
aws rds wait db-instance-available --region $REGION --db-instance-identifier $ID

echo "3/3 Listo. Pasale este valor a Claude:"
echo "RDS_ENDPOINT=$(aws rds describe-db-instances --region $REGION --db-instance-identifier $ID \
  --query 'DBInstances[0].Endpoint.Address' --output text)"
