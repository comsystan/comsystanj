<link rel="shortcut icon" type="image/png" href="favicon.png">

<!-- ![Image](comsystan-logo.png)-->
[<img src="images/comsystan-longlogo-grey46.png" width=640 height=100/>](https://comsystan.github.io/comsystanj)

**ComsystanJ** (Complex Systems Analysis for ImageJ) is a collection of Fiji/ImageJ2 plugins to compute the complexity, fractal dimension, entropy and other nonlinear measures of images and signals. It is recommended to use it with Fiji. Developed and maintained by Helmut Ahammer.

### Downloads
- [Downloads](https://github.com/comsystan/comsystanj/releases)

### Installation
- Unzip the ComsystanJ-x.x.x.zip file and copy the folder to the Fiji's plugins folder. Alternatively, the .jar files can be imported using the Fiji command Plugins/Install. 

### Project descriptions
- Processing signals [- Description of plugins](description/sig-description.md) 
- Processing 2D images [- Description of plugins](description/img2-description.md) 
- Processing 3D images(volumes) [- Description of plugins](description/img3-description.md) 

### Citing ComsystanJ 
A manuscript describing ComsystanJ has already been submitted. If you use ComsystanJ plugins in the meantime and publish your work, please cite at least one of the following publications:

###### Time series
- Müller et al., Scientific Reports, 2017, [https://doi.org/10.1038/s41598-017-02665-5](https://doi.org/10.1038/s41598-017-02665-5)
- Ahammer et al., Front.Physiol., 2018, [https://doi.org/10.3389/fphys.2018.00546](https://doi.org/10.3389/fphys.2018.00546)

###### Image processing
- Ahammer, PLoS ONE, 2011, [https://doi.org/10.1371/journal.pone.0024796](https://doi.org/10.1371/journal.pone.0024796)
- Ahammer et.al., Chaos, 2015, [https://doi.org/10.1063/1.4923030](https://doi.org/10.1063/1.4923030)
- Mayrhofer-Reinhartshuber & Ahammer, Chaos, 2016, [https://doi.org/10.1063/1.4958709](https://doi.org/10.1063/1.4958709)
- Kainz et.al., PLoS ONE, 2015, [https://doi.org/10.1371/journal.pone.0116329](https://doi.org/10.1371/journal.pone.0116329)

### Collaborations

[<img src="images/nasa-logo.png" width=60 height=50/>](https://www.nasa.gov/goddard) [<img src="images/nasa-ccmc-logo.png" width=117 height=45/>](https://ccmc.gsfc.nasa.gov/) [CCMC](https://ccmc.gsfc.nasa.gov/)<br/>
A multi-agency partnership enabling, supporting, and performing research and development for next-generation space science and space weather models.
The CCMC is situated at NASA Goddard Space Flight Center [GSFC](https://www.nasa.gov/goddard)

[<img src="images/caimt-logo.png" width=58 height=56/>](https://caimt.ro) [CAIMT](https://caimt.ro)<br/>
The Research Center for Integrated Analysis and Territorial Management aims to develop methods for advanced modeling of the complexity of territorial systems.

[<img src="images/iors-logo.png" width=51 height=51/>](https://iors.ro) [IORS](https://www.ncrc.ac.rs)<br/>
The Institute for Oncology and Radiology of Serbia is a cancer clinic integrated with the National Cancer Research Centre as a research unit.

[<img src="images/noologica-logo.png" width=222 height=37/>](https://noologica.com) [Noologica](https://noologica.com)<br/>
An open source initiative for the design, development, testing, validation and dissemination of a mental health research-diagnostic application.

[<img src="images/iasms-logo.png" width=100 height=31/>](https://iasms.org) [IASMS](https://iasms.org)<br/>
The International Association of Sciences in Medicine and Sports is dedicated to scientific activities in all fields of medicine by maintaining health.

[<img src="images/nisos-logo.png" width=100 height=48/>](https://nisos.at) [NISOS](https://nisos.at)<br/>
Body composition analyses

[<img src="images/mug-logo.png" width=58 height=47/>](https://medunigraz.at/en) [MUG](https://medunigraz.at/en)<br/>
Medical University of Graz, Division of Medical Physics & Biophysics

### Notes for using ComsystanJ with Fiji

**If image plugins do not work, select Edit/Options/ImageJ2..., activate "Use SCIFIO when opening files" and restart Fiji.**
  
**If signal plugins do not work, check that the file scijava-plugins-io-table-x.x.x.jar in the jars folder is at least version 0.4.0.**

**It is recommended to use the latest Fiji version from the download archive [https://downloads.imagej.net/fiji/archive](https://downloads.imagej.net/fiji/archive). Updating is not mandatory.**

**Please note that updating an older Fiji version is sometimes not successful to get the latest version**

It is recommended to use both ComsystanJ and Fiji with the same pom-scijava version number.
The pom-scijava version number of ComsystanJ can be viewed directly on the download site [Downloads](https://github.com/comsystan/comsystanj/releases).
To get the pom-scijava version number of your Fiji, simply start Fiji and execute following command in the search bar: `!ui.showDialog(app.getApp("Fiji").getPOM().getParentVersion())`

### Contact
Helmut Ahammer
e-mail **helmut.ahammer@medunigraz.at**
