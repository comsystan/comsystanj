## Image(3D) - Short description of plugins

### Image volume generator
- Generates a single image volume
- 8bit grey or RGB color
- Volume size can be set
- Maximal grey values can be set
- Random, Gaussian, Constant
- Fractal surface - Fourier (FFT) or Midpoint displacment (MPD)
- - Theoretical fractal dimension in the range of [3,4] can be set
- Menger cube or Sierpinski pyramid
- - Width is taken for the height and depth
- Note: Fiji sometimes displays oversaturated grey values. Workaround: Image/Color/Edit LUT... and press 2x Invert

### 3D Higuchi dimension
- Fractal dimension is computed with Higuchi inspired 3D algorithms
- This is a direct expansion of the 2D algorithms
- 8-bit grey
- Several options can be chosen
- Linear regression parameters of the double log plot can be set
- Ref.: Ahammer et al., Chaos, 2015, [DOI 10.1063/1.4923030](https://doi.org/10.1063/1.4923030)

### 3D FFT dimension
- Fractal dimension is computed with 3D FFT algorithm
- 8-bit grey images
- Several windowing filters can be set
- Spherical average of k values or
- Mean of separate line scans (horizontal and verical) or
- Integrated line scans (k should be restricted to low values - frequencies)
- Dt=3, Topological dimension is assumed to be 3
- Linear regression parameters of the double log plot can be set
- For spherical averaging, the number of regression points is higher than k itself and additionally, will be automatically lowered to the number of averages.  
