## Image(3D) - Short description of plugins

### 3D Image volume generator
- Generates a single image volume
- 8bit grey or RGB color
- Volume size can be set
- Maximal grey values can be set
- Random, Gaussian, Constant
- Fractal surface - Fourier (FFT) or Midpoint displacement (MPD)
  - Theoretical fractal dimension in the range of [3,4] can be set
- Fractal Iterated function system (IFS) - Menger cube, Sierpinski pyramid or Mandelbrot island
  - The number of IFS iterations can be set
  - Width is taken for the height and depth of the volume
- Note: Image volumes can be visualized with Fiji's 3D viewer (Plugins/3D Viewer)
- Note: Fiji sometimes displays oversaturated grey values. Workaround: Image/Color/Edit LUT... and press 2x Invert

### 3D Higuchi dimension
- Fractal dimension is computed with Higuchi inspired 3D algorithms
- This is a direct expansion of the 2D algorithms
- 8-bit grey images
- Several options can be chosen
- Linear regression parameters of the double log plot can be set
- Ref.: Ahammer et al., Chaos, 2015, [DOI 10.1063/1.4923030](https://doi.org/10.1063/1.4923030)

### 3D FFT dimension
- Fractal dimension is computed with a 3D FFT algorithm or subsequent 1D FFTs
- 8-bit grey images
- Several windowing filters can be set
- Spherical average of k values or
- Mean of separate line scans (horizontal and verical) or
- Integrated line scans (k should be restricted to low values - frequencies)
- Dt=3, Topological dimension is assumed to be 3
- Linear regression parameters of the double log plot can be set
- For spherical averaging, the number of regression points is higher than k itself and additionally, will be automatically lowered to the number of averages.  

### 3D Tug of war dimension
- Fractal dimension is computed by using the 3D tug of war algorithm
- 8bit binary images
- Binary [0, >0] algorithm
- The number of boxes with distinct sizes according to the power of 2 can be set
- Linear regression parameters of the double log plot can be set
- The ToW algorithm is a statistical approach and is dependent on the accuracy and confidence settings
- In the original paper accuracy=30 and confidence=5
- But it is recommended to set accuracy and confidence as high as computation times allow
- Ref.: Reiss et al., Chaos, 2016, DOI 10.1063/1.4966539
