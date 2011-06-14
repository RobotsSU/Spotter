mkdir ../res/raw
grxmlcompile -par ./baseline11k.par -grxml ./$1.grxml -outdir ./
make_g2g -base ./$1,addWords=0 -out ../res/raw/$1.g2g
