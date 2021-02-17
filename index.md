<link rel="shortcut icon" type="image/png" href="{{site.url}}favicon.png">

<!-- ![Image](comsystan-logo.png)-->
<img src="images/comsystan-logo-grey46.png" width=100 height=100/>

**ComsystanJ** (Complex Systems Analysis for ImageJ) is a collection of ImageJ2 plugins to compute the complexity, fractal dimension and other nonlinear measures of images and signals. Copyright (c) Comsystan Software. Developed and maintained by Helmut Ahammer.

If you use ComsystanJ plugins and publish your work, please cite at least one of the following publications:
- Ahammer, PLoS ONE, 2011, DOI 10.1371/journal.pone.0024796
- Ahammer etal., Chaos, 2015, DOI 10.1063/1.4923030
- Mayrhofer-Reinhartshuber & Ahammer, Chaos, 2016, DOI 10.1063/1.4958709
- Kainz etal., PLoS ONE, 2015, DOI 10.1371/journal.pone.0116329


### Project descriptions
- Processing 2D images [- Short description of plugins](description/img2-description.md) 

- Processing 3D images(volumes) [- Short description of plugins](description/img3-description.md) 

- Processing signals [- Short description of plugins](description/sig-description.md) 

### Note for using ComsystanJ jar file plugins with Fiji

It is recommended to use ComsystanJ jar files and a Fiji distribution with the same pom-scijava version number.

ComsystanJ releases have a 4-digit version number in the jar file names as defined in the corresponding pom.xml files. The first three digits reflect the pom-scijava version. The last digit is the ComsystanJ version number.
To get the version number of your Fiji, simply start Fiji and execute following command in the search bar: `!ui.showDialog(app.getApp("Fiji").getPOM().getParentVersion())`
