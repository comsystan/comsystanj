<link rel="shortcut icon" type="image/png" href="favicon.png">

<!-- ![Image](comsystan-logo.png)-->
[<img src="images/comsystan-longlogo-grey46.png" width=640 height=100/>](https://comsystan.github.io/comsystanj)

**ComsystanJ** (Complex Systems Analysis for ImageJ) is a collection of Fiji/ImageJ2 plugins to compute the complexity, fractal dimension, entropy and other nonlinear measures of images and signals. It is recommended to use it with Fiji. Developed and maintained by Helmut Ahammer.

### Downloads
- [Downloads](https://github.com/comsystan/comsystanj/releases)

### Installation
- Copy the ComsystanJ-x.x.x.jar file to the Fiji's plugins folder. Alternatively, the jar file can be imported using the Fiji command Plugins/Install. 

### Project descriptions
- 1D signals [- Description of 1D plugins](description/Description1DSignal.md) 
- 2D images [- Description of 2D plugins](description/Description2DImage.md) 
- 3D image volume [- Description of 3D plugins](description/Description3DVolume.md) 

### Citing ComsystanJ 
If you use ComsystanJ plugins and publish your work, please cite following publication:

Helmut Ahammer, Martin A. Reiss, Moritz Hackhofer, Ion Andronache, Marko Radulovic, Fabián Labra-Spröhnle, and Herbert Franz Jelinek. „ComsystanJ: A Collection of Fiji/ImageJ2 Plugins for Nonlinear and Complexity Analysis in 1D, 2D and 3D“. PLOS ONE 18, Nr. 10, 2023, e0292217, [https://doi.org/10.1371/journal.pone.0292217](https://doi.org/10.1371/journal.pone.0292217)

### Notes for using ComsystanJ with Fiji

**If image plugins do not work, select Edit/Options/ImageJ2..., activate "Use SCIFIO when opening files" and restart Fiji.**
  
**It is recommended to use the latest Fiji version from the download archive [https://downloads.imagej.net/fiji/archive/latest/](https://downloads.imagej.net/fiji/archive/latest/)**

**Signal plugins do not work with Fiji <=2.9.0.**

It is recommended to use both ComsystanJ and Fiji with the same pom-scijava version number.
The pom-scijava version number of ComsystanJ can be viewed directly on the download site [Downloads](https://github.com/comsystan/comsystanj/releases).
To get the pom-scijava version number of your Fiji, simply start Fiji and execute following command in the search bar: `!ui.showDialog(app.getApp("Fiji").getPOM().getParentVersion())`

### Contact
Helmut Ahammer

e-mail **comsystan@outlook.com**
