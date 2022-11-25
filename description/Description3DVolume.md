## 3D Volume - Description of plugins

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

### 3D Box counting dimension
- Fractal dimension is computed with 3D box counting
- This is a direct expansion of the 2D algorithm
- 8-bit grey image volume
- Binary [0, >0] algorithm
- Raster box scanning
- The number of boxes with distinct sizes according to the power of 2 can be set
- Linear regression parameters of the double log plot can be set

### 3D Minkowski dimension
- Fractal dimension is computed with 3D morphological dilations and erosions
- 8-bit binary or grey image volume
- Binary [0, >0] dilation
- Blanket or Variation method with grey value dilation/erosion
- The number of dilation/erosion steps can be set
- The shape of the morphological structuring element can be set
- Linear regression parameters of the double log plot can be set 
- Ref.: Peleg et al., IEEE-TPAMI, 1984, [DOI 10.1109/TPAMI.1984.4767557](https://doi.org/10.1109/TPAMI.1984.4767557)

### 3D Correlation dimension
- Fractal dimension is computed with 3D correlation
- This is a direct expansion of the 2D algorithm
- 8-bit grey image volume
- Binary [0, >0] or grey value correlation
- Fast Raster box scanning instead of slow sliding box scanning
- The number of boxes with distinct sizes according to the power of 2 can be set
- Linear regression parameters of the double log plot can be set

### 3D Generalized dimensions
- 3D Generalized fractal dimensions are computed
- 8-bit binary or grey image volume
- Binary [0, >0] or grey value mass algorithm
- Raster box scanning
- Dq and f-spectrum plots can be shown
- The number of boxes with distinct sizes according to the power of 2 can be set
- Linear regression parameters of the double log plot can be set
- Ref: Ahammer et al., Physica D, 2003, DOI 10.1016/S0167-2789(03)00099-X

### 3D Higuchi dimension
- Fractal dimension is computed with Higuchi inspired 3D algorithms
- This is a direct expansion of the 2D algorithms
- 8-bit grey image volume
- Grey value algorithm
- Several options can be chosen
- Linear regression parameters of the double log plot can be set
- Ref.: Ahammer et al., Chaos, 2015, [DOI 10.1063/1.4923030](https://doi.org/10.1063/1.4923030)

### 3D FFT dimension
- Fractal dimension is computed with a 3D FFT algorithm or subsequent 1D FFTs
- 8-bit grey image volume
- Several windowing filters can be set
- Spherical average of k values or
- Mean of separate line scans (horizontal and verical) or
- Integrated line scans (k should be restricted to low values - frequencies)
- Dt=3, Topological dimension is assumed to be 3
- Linear regression parameters of the double log plot can be set
- For spherical averaging, the number of regression points is higher than k itself and additionally, will be automatically lowered to the number of averages.  

### 3D Tug of war dimension
- Fractal dimension is computed by using the 3D tug of war algorithm
- 8bit binary image volume
- Binary [0, >0] algorithm
- Grey value algorithm
- The number of boxes with distinct sizes according to the power of 2 can be set
- Linear regression parameters of the double log plot can be set
- The ToW algorithm is a statistical approach and is dependent on the accuracy and confidence settings
- In the original paper accuracy=30 and confidence=5
- But it is recommended to set accuracy and confidence as high as computation times allow
- Ref.: Reiss et al., Chaos, 2016, DOI 10.1063/1.4966539

### 3D Lacunarity
- This is a direct expansion of the 2D algorithms
- 8-bit binary or grey image volume
- The number of boxes with distinct sizes can be set
- Shows a double logarithmic plot of lacunarities
- Raster/Sliding box scanning or Tug of war method
- Binary [0, >0] or grey value algorithm
- Raster box scanning
- \<L\>-R&P... Weighted mean lacunarity according to Roy & Perfect, Fractals, 2014, [DOI10.1142/S0218348X14400039](https://doi.org/10.1142/S0218348X14400039)
- \<L\>-S&V... Weighted mean lacunarity according to Sengupta & Vinoy, Fractals, 2006, [DOI 10.1142/S0218348X06003313](https://doi.org/10.1142/S0218348X06003313)
- Ref.: Reiss et al., Chaos, 2016, [DOI 10.1063/1.4966539](https://doi.org/10.1063/1.4966539)

### 3D Fractal fragmentation indices
- Fractal fragmentation index FFI
- Fractal fragmentation and disorder index FFDI
- 8-bit binary image volume
- Binary [0, >0] algorithm
- Raster box scanning
- FFI = FD of mass - FD of boundary
- FFI is the Box counting dimension of the volume (mass) - Box counting dimension of the boundary volume
- Boundary volume = volume - Eroded volume (erosion by one pixel)
- FFDI = D1(1-FFI)
- D1 is the Information dimension
- Fractal dimension computations with 3D Box counting (raster box scanning)
- The number of boxes with distinct sizes according to the power of 2 can be set
- Linear regression parameters of the double log plot can be set 
- Ref: Andronache et al., Chaos, Solitons & Fractals, 2016, [DOI 10.1016/j.chaos.2016.06.013](https://doi.org/10.1016/j.chaos.2016.06.013)

### 3D Kolmogorov complexity and Logical depth
- KC is estimated in a fast way by compressing data bytes (ZIP, ZLIB, GZIB)
- or
- KC is estimated by the memory size of compressed images saved to disk (TIFF-LZW) - very slow!
- 8-bit grey image volume
- Lossless and lossy algorithms can be chosen
- Lossless algorithms are recommended
- LD is estimated by the decompression time of the compressed data bytes (ZLIB, GZIB)
- LD is estimated by the opening time of the compressed image (LZW, PNG, J2K, JPG)
- Iterations should be set to as high a value as possible
- LD values should be taken with caution, as computers are not well suited to measure times
- Ref.: Zenil et al., Complexity, 2012, [DOI 10.1002/cplx.20388](https://doi.org/10.1002/cplx.20388)
