<link rel="shortcut icon" type="image/png" href="favicon.png">

<!-- ![Image](comsystan-logo.png)-->
<img src="images/comsystan-logo-grey46.png" width=100 height=100/>

**ComsystanJ** (Complex Systems Analysis for ImageJ) is a collection of ImageJ2 plugins to compute the complexity, fractal dimension and other nonlinear measures of images and signals. Copyright (c) Comsystan Software. Developed and maintained by Helmut Ahammer.

### Downloads
- You can download ComsystanJ [here](downloads/index.md) 
- Unzip the file and copy the folder to the Fiji's plugin folder 

### Project descriptions
- Processing 2D images [- Short description of plugins](description/img2-description.md) 

- Processing 3D images(volumes) [- Short description of plugins](description/img3-description.md) 

- Processing signals [- Short description of plugins](description/sig-description.md) 

If you use ComsystanJ plugins and publish your work, please cite at least one of the following publications:

###### Image processing
- Ahammer, PLoS ONE, 2011, [https://doi.org/10.1371/journal.pone.0024796](https://doi.org/10.1371/journal.pone.0024796)
- Ahammer et.al., Chaos, 2015, [https://doi.org/10.1063/1.4923030](https://doi.org/10.1063/1.4923030)
- Mayrhofer-Reinhartshuber & Ahammer, Chaos, 2016, [https://doi.org/10.1063/1.4958709](https://doi.org/10.1063/1.4958709)
- Kainz et.al., PLoS ONE, 2015, [https://doi.org/10.1371/journal.pone.0116329](https://doi.org/10.1371/journal.pone.0116329)

###### Time series
- Ahammer et al., Front.Physiol., 2018, [https://doi.org/10.3389/fphys.2018.00546](https://doi.org/10.3389/fphys.2018.00546)

### Note for using ComsystanJ jar file plugins with Fiji

It is recommended to use ComsystanJ jar files and a Fiji distribution with the same pom-scijava version number.

ComsystanJ releases have a 4-digit version number in the jar file names as defined in the corresponding pom.xml files. The first three digits reflect the pom-scijava version. The last digit is the ComsystanJ version number.
To get the version number of your Fiji, simply start Fiji and execute following command in the search bar: `!ui.showDialog(app.getApp("Fiji").getPOM().getParentVersion())`

### Contact
Helmut Ahammer
e-mail com.syst.an@gmail.com
