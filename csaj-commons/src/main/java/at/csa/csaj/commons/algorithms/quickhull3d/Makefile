default: build

.PHONY: build
build:
	javac -source 1.7 -target 1.7 *.java

install: build
	cd ..; jar cvf lib/quickhull3d.jar quickhull3d doc/quickhull3d
	cd ..; tar czvf lib/quickhull3d.jar.gz quickhull3d doc/quickhull3d
	cd ..; zip -r lib/quickhull3d quickhull3d doc/quickhull3d

