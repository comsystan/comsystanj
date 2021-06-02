## Image(2D) - Short description of plugins

### Fractal Dimension - Box counting
- Fractal dimension is computed with box counting
- For binary or grey 8-bit images
- Binary [0, >0] counting or DBC and RDBC
- Raster box scanning
- The number of boxes with distinct sizes according to the power of 2 can be set
- Linear regression parameters of the double log plot can be set 

### Fractal Dimension - Correlation
- Fractal correlation dimension is computed
- For binary 8-bit images
- Binary [0, >0] algorithm
- Classical pair wise occurrence counting (Sliding box scanning)
- Computation times can be lowered by decreasing the Pixel% (% of randomly chosen image pixels)
- or by using a fixed grid estimation of summing up squared counts (Raster box scanning)
- The number of boxes with distinct sizes according to the power of 2 can be set
- Linear regression parameters of the double log plot can be set 
- Ref: Grassberger & Procaccia, Physica D, 1983, [https://10.1016/0167-2789(83)90298-1](https://10.1016/0167-2789(83)90298-1)

### Fractal Dimension - Directional correlation
- A directional dependent fractal correlation dimension is computed
- For binary 8-bit images
- Classical pair wise occurrence counting
- Binary [0, >0] algorithm
- Directions can be set
- Horizontal & vertical, 4 radial directions [0-180°], 180 radial directions [0-180°] 
- Computation times can be lowered by decreasing the Pixel% (% of randomly chosen image pixels)
- The number of distances with distinct sizes according to the power of 2 can be set
- Linear regression parameters of the double log plot can be set 

### Fractal Dimension - FFI
- Fractal fragmentation index
- For binary 8-bit images
- Binary [0, >0] algorithm
- FFI = FD of mass - FD of boundary
- FFI is the fractal dimension of the image - Fractal dimension of the boundary image
- Boundary image = Image - Eroded image (erosion by one pixel)
- Fractal dimension computations with Box counting (raster box scanning) or Image pyramid algorithm
- The number of boxes/pyramid images with distinct sizes according to the power of 2 can be set
- Linear regression parameters of the double log plot can be set 
- Ref: Andronache et al., Chaos, Solitons & Fractals, 2016, [https://doi.org/10.1016/j.chaos.2016.06.013](https://doi.org/10.1016/j.chaos.2016.06.013)

### Fractal Dimension - Generalized
- Generalized fractal dimensions are computed
- For binary 8-bit images
- Binary [0, >0] algorithm
- Raster or sliding box scanning
- Sliding box computation times can be lowered by decreasing the Pixel% (% of randomly chosen image pixels)
- Dq and f-spectrum plots can be shown
- The number of boxes with distinct sizes according to the power of 2 can be set
- Linear regression parameters of the double log plot can be set 
- Ref: Ahammer et al., Physica D, 2003, [https://doi.org/10.1016/S0167-2789(03)00099-X](https://doi.org/10.1016/S0167-2789(03)00099-X)

### Fractal Dimension - Higuhi1D
- Fractal dimension is computed for 1D grey value profiles extracted from an image
- For 8-bit grey value images
- Several extraction methods can be chosen
- Single centered row/column, Single meander row/column, Mean of all rows/columns, 4 radial lines [0-180°], 180 radial lines [0-180°] 
- Angle extractions use an interpolated image surface that introduce some errors
- Linear regression parameters of the double log plot can be set
- Ref.: Ahammer, PLoS ONE, 2011, [https://doi.org/10.1371/journal.pone.0024796](https://doi.org/10.1371/journal.pone.0024796)

### Fractal Dimension - Higuchi2D
- Fractal dimension is computed with Higuchi inspired 2D algorithms
- For 8-bit grey value images
- Several options can be chosen
- Linear regression parameters of the double log plot can be set
- Ref.: Ahammer et al., Chaos, 2015, [https://doi.org/10.1063/1.4923030](https://doi.org/10.1063/1.4923030)

### Fractal Dimension - Pyramid algorithm
- Fractal dimension is computed by using image pyramids
- For binary 8-bit images
- Binary [0, >0] algorithm
- Linear regression parameters of the double log plot can be set
- Number of object pixels is counted for subsequently size reduced images
- Results are identical to the common Box Counting algorithm for quadratic images with size 2^n
- For other sizes it yields more reliable results, because box truncation is not necessary
- Ref.: Mayrhofer-Reinhartshuber & Ahammer, Chaos, 2016, [https://doi.org/10.1063/1.4958709](https://doi.org/10.1063/1.4958709)

### Fractal Dimension - Tug of war algorithm
- Fractal dimension is computed by using a tug of war algorithm
- For binary 8-bit images
- Binary [0, >0] algorithm
- The number of boxes with distinct sizes according to the power of 2 can be set
- Linear regression parameters of the double log plot can be set 
- The ToW algorithm is a statistical approach and is dependent on the accuracy and confidence settings
- In the original paper accuracy=30 and confidence=5  
- But it is recommended to set accuracy and confidence as high as computation times allow
- Ref.: Reiss et al., Chaos, 2016, [https://doi.org/10.1063/1.4966539](https://doi.org/10.1063/1.4966539)

### Image generator
- Generates a single image or an image stack
- 8bit Grey images or Color RGB images
- Image size can be set
- Maximal grey values can be set
- Random, Gaussian, Sine - radial, Sine - horizontal, Sine - vertical,  Constant
- Freqeuncy can be set for Sine
- Fractal surface - Fourier (FFT) or Midpoint displacment (MPD) or Sum of sine method
- Theoretical fractal dimension in the range of [2,3] can be set
- For Sum of sine the frequency, amplitude and number of iterations can be set
- Fractal Iterated function system (IFS) - Menger, Sierpinski-1, Sierpinski-2, Koch snowflake, Fern, Heighway dragon
- For the IFS the number of iterations can be set
- The polygon number for the Koch snowflake can be set
- Note: The number of iterates must be really high for the Fern 
- Note: Fiji displays RGB images as 3 channel color images. Workaround: Image/Type/RGB Color 
- Note: Fiji sometimes displays an enhanced R channel. Workaround: Image/Color/Arrange Channels... and press OK

### Image opener
- Opens a single image or an image stack
- Grey 8bit or Color RGB images are supported
- For a stack, all images must be of same size and type  
- Includes a preview (thumbnail) panel
- Note: Fiji displays RGB images as 3 channel color images. Workaround: Image/Type/RGB Color 
- Note: Fiji sometimes displays an enhanced R channel. Workaround: Image/Color/Arrange Channels... and press OK

### Kolmogorov complexity and Logical depth
- KC is estimated by the memory size of compressed images saved to disk (LZW, PNG, J2K, JPG)
- KC is estiamted by compressing data bytes (ZLIB, GZIB)
- For 8-bit grey value images
- RGB color images may also work, but not tested
- Lossless and lossy algorithms can be chosen
- Lossless algorithms are recommended.
- LD is estimated by the opening time of the compressed image (LZW, PNG, J2K, JPG)
- LD is estimated by the decompression time of the compressed data bytes (ZLIB, GZIB)
- Iterations should be set to as high a value as possible.
- LD values should be taken with caution, as computers are not well suited to measure times
- Ref.: Zenil et al., Complexity, 2012, [https://doi.org/10.1002/cplx.20388](https://doi.org/10.1002/cplx.20388)

### Lacunarity
- Lacunarity is computed of a binary image  
- For binary or grey 8-bit images
- The number of boxes with distinct sizes can be set
- Linear regression parameters can be set 
- Raster/Sliding box scanning or Tug of war method
- Binary [0, >0] algorithm for Raster/Sliding box and Tug of war method
- Grey value algortihm for Raster/Sliding box
- Sliding box computation times can be lowered by decreasing the Pixel% (% of randomly chosen image pixel)
- \<L\>-R&P Weighted mean lacunarity according to Roy & Perfect, Fractals, 2014, [https://doi.org/10.1142/S0218348X14400039](https://doi.org/10.1142/S0218348X14400039)
- \<L\>-S&V Weighted mean lacunarity according to Sengupta & Vinoy, Fractals, 2006, [https://doi.org/10.1142/S0218348X06003313](https://doi.org/10.1142/S0218348X06003313)
- The ToW algorithm is a statistical approach and is dependent on the accuracy and confidence settings
- In the original paper accuracy=30 and confidence=5  
- But it is recommended to set accuracy and confidence as high as computation times allow
- Ref.: Reiss et al., Chaos, 2016, [https://doi.org/10.1063/1.4966539](https://doi.org/10.1063/1.4966539)

### Succolarity
- Succolarity is computed by flooding of a binary image  
- For binary 8-bit images
- Binary [0, >0] algorithm
- The number of boxes with distinct sizes can be set
- Linear regression parameters can be set 
- Raster box or Sliding box scanning
- Binary [0, >0] algorithm
- Flooding of image can be set to Top2down, Down2top, Left2right, Right2left or the mean of all four directions
- Ref.: de Melo & Conci, Telecommunication Systems, 2013, [https://doi.org/10.1007/s11235-011-9657-3](https://doi.org/10.1007/s11235-011-9657-3)
