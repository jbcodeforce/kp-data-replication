
STRIMZI_FOLDER=~/Code/Studies/strimzi-0.17.0
NSPACE=strimzi
INSTALLDIR=$STRIMZI_FOLDER/install
TGTDIR=$NSPACE/cluster-operator
mkdir $NSPACE
mkdir $TGTDIR
cp $INSTALLDIR/cluster-operator/*.yaml $TGTDIR

sed -i '' "s/namespace: .*/namespace: "$NSPACE"/"  $TGTDIR/*RoleBinding*.yaml

oc apply -f $TGTDIR/

