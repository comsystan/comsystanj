## Image(3D) - Short description of plugins

### Image volume generator
- Generates a single image volume
- 8bit grey
- Volume size can be set
- Maximal grey values can be set
- Random, Gaussian, Constant
- Fractal surface - Fourier (FFT) or Midpoint displacment (MPD)
- - Theoretical fractal dimension in the range of [3,4] can be set
- Note: Fiji sometimes displays oversaturated grey values. Workaround: Image/Color/Edit LUT... and press 2x Invert

### Higuchi dimension 3D
- Fractal dimension is computed with Higuchi inspired 3D algorithms
- This is a direct expansion of the 2D algorithms
- 8-bit grey
- Several options can be chosen
- Linear regression parameters of the double log plot can be set
- Ref.: Ahammer et al., Chaos, 2015, [DOI 10.1063/1.4923030](https://doi.org/10.1063/1.4923030)
