## 3D Volume - Description of plugins


### 3D Image volume opener
- Opens an image stack treated as volume
- 8-bit grey or RGB color images
- For a stack, all images must be of same size and type  
- Includes a preview (thumbnail) panel
- Note: Fiji displays RGB images as 3 channel color images. Workaround: Type Image/Type/RGB Color 

### 3D Image volume generator
- Generates a single image volume
- 8-bit grey or RGB color
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

### 3D Preprocessing - 3D Filter
- Image volume filtering
- 8-bit grey or RGB color image volume
- Gaussian blur
  - Sigma can be set
- Mean, Median
  - Size of kernel can be set
- Low-pass, High-pass with FFT
  - Radius (Cutoff frequency) can be set

### 3D Preprocessing - 3D Noise
- Adding noise
- 8-bit grey or RGB color image volume
- Shot, Salt&Pepper
  - Percentage of pixels that will be changed can be set
- Uniform
  - Percentage of maximum value changes can be set
- Gaussian, Rayleigh, Exponential
  - Scaling parameter can be set

### 3D Preprocessing - 3D Outliers
- Eliminating or extracting outliers — detecting extreme events
- 8-bit grey or RGB color image volume
- Z-score, Interquartile range IQR

### 3D Preprocessing - 3D Surrogates
- Computes a surrogate image volume
- 8-bit grey or RGB color image volume
- Shuffle, Gaussian, Random phase, AAFT
- FFT windowing can be set

### 3D Complexity analyses - 3D Kolmogorov complexity and Logical depth
- KC is estimated in a fast way by compressing data bytes (ZIP, ZLIB, GZIB) or
- KC is estimated by the memory size of compressed images saved to disk (TIFF-LZW) - very slow!
- 8-bit grey image volume
- Lossless and lossy algorithms can be chosen
- Lossless algorithms are recommended
- LD is estimated by the decompression time of the compressed data bytes (ZIP, ZLIB, GZIB) or
- LD is estimated by the opening time of the compressed image (TIFF-LZW)
- Iterations should be set to the highest possible value
- LD values should be taken with caution, as computers are not well suited to measure times
- Zenil et al., Complexity, 2012, [DOI 10.1002/cplx.20388](https://doi.org/10.1002/cplx.20388)

### 3D Complexity analyses - 3D Renyi heterogeneities
- Also known as Hill numbers, Hannah–Kay indices or participation ratio
- Probabilities of grey values or grey value differences 
- 8-bit grey volume
- A plot of Renyi heterogeneities can be shown
- Nunes et al., 2020, J Psychiatry Neurosci, [DOI 10.1503/jpn.190198](https://doi.org/10.1503/jpn.190198)
- Nunes et al., 2020, Translational Psychatry, [DOI 10.1038/s41398-020-00986-0](https://doi.org/10.1038/s41398-020-00986-0)
- Nunes et al., 2020, PLOS One, [DOI 10.1371/journal.pone.0242320](https://doi.org/10.1371/journal.pone.0242320)
- Nunes et al., 2020, Entropy, [DOI 10.3390/e22040417](https://doi.org/10.3390/e22040417)

### 3D Complexity analyses - 3D Statistical complexity measures
- SCM = Shannon entropy H * Distribution distance D
- Probabilities are computed with plain voxel grey values
- 8-bit grey image volume
- _E ... Euclidean distance
- _W ... Wootter's distance
- _K ... Kullback-Leibler distance
- _J ... Jensen-Shannon distance
- SCM_E is also known as LMC complexity
- H and/or D can be normalised
- Kowalski et al., 2011, Entropy, [DOI 10.3390/e13061055](https://doi.org/10.3390/e13061055)
- Das et al., 2024, arXiv, [DOI 10.48550/arXiv.2411.06755](https://doi.org/10.48550/arXiv.2411.06755)
- López-Ruiz et al., 1995, Physics Letters A, [DOI 10.1016/0375-9601(95)00867-5](https://doi.org/10.1016/0375-9601(95)00867-5)
- Wootters, 1981, Physical Review D, [DOI 10.1103/PhysRevD.23.357](https://doi.org/10.1103/PhysRevD.23.357)

### 3D Entropy analyses - 3D Generalised entropies
- SE, H1, H2, H3, Renyi, Tsallis, SNorm, SEscort, SEta, SKappa, SB, SBeta, SGamma
- Probabilities of voxel values or grey value differences
- 8-bit grey image volume
- A plot of Renyi entropies can be shown
- Amigo et al., 2018, Entropy, [DOI 10.3390/e20110813](https://doi.org/10.3390/e201108)
- Tsallis, Introduction to Nonextensive Statistical Mechanics, Springer, 2009

### 3D Fractal analyses - 3D Box counting dimension
- Fractal dimension with 3D box counting
- This is a direct expansion of the 2D algorithm
- 8-bit grey image volume
- Binary [0, >0] algorithm
- Raster box scanning
- The number of boxes with distinct sizes according to the power of 2 can be set
- Linear regression parameters of the double log plot can be set

### 3D Fractal analyses - 3D Correlation dimension
- Fractal dimension with 3D correlation
- This is a direct expansion of the 2D algorithm
- 8-bit grey image volume
- Binary [0, >0] or grey value correlation
- Fast Raster box scanning instead of slow sliding box scanning
- The number of boxes with distinct sizes according to the power of 2 can be set
- Linear regression parameters of the double log plot can be set

### 3D Fractal analyses - 3D FFT dimension
- Fractal dimension with a 3D FFT algorithm or subsequent 1D FFTs
- 8-bit grey image volume
- Several windowing filters can be set
- Spherical average of k values or
- Mean of separate line scans (horizontal and verical) or
- Integrated line scans (k should be restricted to low values - frequencies)
- Dt=3, Topological dimension is assumed to be 3
- Linear regression parameters of the double log plot can be set
- For spherical averaging, the number of regression points is higher than k itself and additionally, will be automatically lowered to the number of averages

### 3D Fractal analyses - 3D Fractal fragmentation indices
- FFI -  Fractal fragmentation index
- FFDI - Fractal fragmentation and disorder index
- FTI -  Fractal tentacularity index
- 8-bit binary image volume
- Binary [0, >0] algorithm
- Raster box scanning
- FFI = FD of mass - FD of boundary
- FFI is the Box counting dimension of the volume (mass) - Box counting dimension of the boundary volume
- Boundary volume = volume - Eroded volume (erosion by one pixel)
- FFDI = D1(1-FFI)
- D1 is the Information dimension
- FTI = FFI(convex hull) - FFI
- Fractal dimension computations with 3D Box counting (raster box scanning)
- The number of boxes with distinct sizes according to the power of 2 can be set
- Linear regression parameters of the double log plot can be set 
- Andronache et al., Chaos, Solitons & Fractals, 2016, [DOI 10.1016/j.chaos.2016.06.013](https://doi.org/10.1016/j.chaos.2016.06.013)

### 3D Fractal analyses - 3D Generalised dimensions
- 3D Generalised fractal dimensions
- 8-bit binary or grey image volume
- Binary [0, >0] or grey value mass algorithm
- Raster box scanning
- Dq and f-spectrum plots can be shown
- The number of boxes with distinct sizes according to the power of 2 can be set
- Linear regression parameters of the double log plot can be set
- Ahammer et al., Physica D, 2003, DOI 10.1016/S0167-2789(03)00099-X

### 3D Fractal analyses - 3D Higuchi dimension
- Fractal dimension with Higuchi inspired 3D algorithms
- This is a direct expansion of the 2D algorithms
- 8-bit grey image volume
- Grey value algorithm
- Several options can be chosen
- Linear regression parameters of the double log plot can be set
- Ahammer et al., Chaos, 2015, [DOI 10.1063/1.4923030](https://doi.org/10.1063/1.4923030)

### 3D Fractal analyses - 3D Lacunarity
- Direct expansion of the 2D algorithms
- 8-bit binary or grey image volume
- The number of boxes with distinct sizes can be set
- Shows a double logarithmic plot of lacunarities
- Raster box scanning (Sliding box and Tug of war method are planned for a future release)
- Binary [0, >0] or grey value algorithm
- Raster box scanning
- \<L\>-R&P... Weighted mean lacunarity according to Roy & Perfect, Fractals, 2014, [DOI10.1142/S0218348X14400039](https://doi.org/10.1142/S0218348X14400039)
- \<L\>-S&V... Weighted mean lacunarity according to Sengupta & Vinoy, Fractals, 2006, [DOI 10.1142/S0218348X06003313](https://doi.org/10.1142/S0218348X06003313)
- Reiss et al., Chaos, 2016, [DOI 10.1063/1.4966539](https://doi.org/10.1063/1.4966539)

### 3D Fractal analyses - 3D Minkowski dimension
- Fractal dimension with 3D morphological dilations and erosions
- 8-bit binary or grey image volume
- Binary [0, >0] dilation
- Blanket or Variation method with grey value dilation/erosion
- The number of dilation/erosion steps can be set
- The shape of the morphological structuring element can be set
- Linear regression parameters of the double log plot can be set 
- Peleg et al., IEEE-TPAMI, 1984, [DOI 10.1109/TPAMI.1984.4767557](https://doi.org/10.1109/TPAMI.1984.4767557)

### 3D Fractal analyses - 3D Succolarity
- Succolarity by flooding the black pixels of a binary image volume  
- 8-bit binary image volume
- Binary [0, >0] algorithm
- The number of boxes with distinct sizes can be set
- Shows a double logarithmic plot of succolarities 
- Raster box (Sliding box scanning not implemented)
- Flooding can be set to Top2Down, Down2Top, Left2Right, Right2Left, Back2Front and Front2Back
- Mean computes the average of all six flooding directions
- Anisotropy in the range [0, 1] is computed in the same way as the fractional anisotropy of diffusion
- Succolarity reservoir is the largest possible flooding area (#black pixels)/(#total pixels)
- Delta succolarity is Succolarity reservoir - Succolarity
- de Melo & Conci, 15th International Conference on Systems, Signals and Image Processing, 2008, [DOI 10.1109/IWSSIP.2008.4604424](https://doi.org/10.1109/IWSSIP.2008.4604424)
- de Melo & Conci, Telecommunication Systems, 2013, [DOI 10.1007/s11235-011-9657-3](https://doi.org/10.1007/s11235-011-9657-3)
- Andronache, Land, 2024, [DOI 10.3390/land13020138](https://doi.org/10.3390/land13020138)

### 3D Fractal analyses - 3D Tug of war dimension
- Fractal dimension by using the 3D tug of war algorithm
- 8-bit binary image volume
- Binary [0, >0] algorithm
- Grey value algorithm
- The number of boxes with distinct sizes according to the power of 2 can be set
- Linear regression parameters of the double log plot can be set
- The ToW algorithm is a statistical approach and is dependent on the accuracy and confidence settings
- In the original paper accuracy=30 and confidence=5
- But it is recommended to set accuracy and confidence as high as computation times allow
- Reiss et al., Chaos, 2016, DOI 10.1063/1.4966539
