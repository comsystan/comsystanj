## Image(2D) - Short description of plugins

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

### Fractal Dimension - Higuhi1D
- Fractal dimensions are computed for 1D grey value profiles extracted from an image
- Uses grey values of 8-bit images.
- Several extraction methods can be chosen.
- Angle extractions use the Bresenham algorithm that introduce some interpolation errors
- Ref.: Ahammer, PLoS ONE, 2011, [https://doi.org/10.1371/journal.pone.0024796](https://doi.org/10.1371/journal.pone.0024796)

### Fractal Dimension - Higuchi2D
- Fractal dimension is computed with Higuchi inspired 2D algorithms
- Uses grey values of 8-bit images
- Several options can be chosen
- Ref.: Ahammer etal., Chaos, 2015, [https://doi.org/10.1063/1.4923030](https://doi.org/10.1063/1.4923030)

### Fractal Dimension - Pyramid algorithm
- Fractal dimension is computed by using image pyramids
- For binary 8-bit images
- Number of object pixels is counted for subsequently size reduced images
- Results are identical to the common Box Counting algorithm for quadratic images with size 2^n
- For other sizes it yields more reliable results, because box truncation is not necessary
- Ref.: Mayrhofer-Reinhartshuber & Ahammer, Chaos, 2016, [https://doi.org/10.1063/1.4958709](https://doi.org/10.1063/1.4958709)

### Kolmogorov complexity and Logical Depth
- KC is estimated by the memory size of compressed images saved to disk (LZW, PNG, J2K, JPG)
- KC is estiamted by compressing data bytes (ZLIB, GZIB)
- For 8-bit grey value images.
- RGB color images may also work, but not tested
- Lossless and lossy algorithms can be chosen
- Lossless algorithms are recommended.
- LD is estimated by the opening time of the compressed image (LZW, PNG, J2K, JPG)
- LD is estimated by the decompression time of the compressed data bytes (ZLIB, GZIB)
- Iterations should be set to as high a value as possible.
- LD values should be taken with caution, as computers are not well suited to measure times
- Ref.: Zenil etal., Complexity, 2012, [https://doi.org/10.1002/cplx.20388](https://doi.org/10.1002/cplx.20388)

