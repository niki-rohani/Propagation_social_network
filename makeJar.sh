javac -d bin -classpath "lib/*:bin/*" src/**/*.java src/**/**/*.java src/**/**/**/*.java
echo Main-Class: $1 > MANIFEST.MF
cat ManifestClassPath.txt >> MANIFEST.MF
jar cvmf MANIFEST.MF Main.jar bin
export LD_PRELOAD=/usr/lib/x86_64-linux-gnu/libcuda.so
echo  java -Xincgc  -Xms1G -Xmx2G -Djava.library.path=/usr/lib/x86_64-linux-gnu/:./lib/ -jar Main.jar \$1 \$2 \$3 \$4 \$5 \$6 \$7 \$8 \$9 \$10 \$11 \$12 \$13 \$14 \$15 \$16 \$17 \$18 \$19 \$20 \$21 \$22 \$23 \$24 \$25 \$26 \$27 \$28 \$29 \$30 > main.sh 
