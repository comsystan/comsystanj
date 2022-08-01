## Signal - Description of plugins

### Signal opener
- Opens signals or plots of floating values (dot decimal numbers)
- For comma delimited text files
- The first row must contain the column (signal) headers
- Maximal row number is 2147483647 or 2^31-1
- The first column must contain the row headers or just a subsequent numbering 
- All columns MUST have the same number of rows
- Missing values MUST be filled up with NaN
- NaNs will be ignored for plot charts
- NaNs will be ignored by CSAJ signal plugins
- Conversion of integer to floating numbers with Notepad++ can be found [here](notepadpp/IntegerToFloating.md) 
- Filling up missing values by NaNs with Notepad++ can be found [here](notepadpp/FillNaNs.md) 
- A sample file can be donwloaded [here](samples/SignalSample.txt)

### Signal generator
- Generates signals of floating values (dot decimal numbers)
- Generates a table for further processing
- Following signal types are supported:
  - Constant, Sine, Square, Triangle, Sawtooth
  - Gaussian and Uniform noise
  - Discrete chaotical maps (Logistic, Henon, Cubic, Spence), Silva & Murta Jr., 2012, Chaos, [DOI 10.1063/1.4758815](http://dx.doi.org/10.1063/1.4758815) 
  - Fractional Gaussian noise with variable Hurst coefficient using Davis and Harte autocorrelation method DHM
  - Fractional Gaussian motion with variable Hurst coefficient using spectral synthesis method SSM, Eke et al., 2000, Pflugers Archiv-European Journal of Physiology, [DOI 10.1007/s004249900135](https://doi.org/10.1007/s004249900135) Caccia et.al., 1997, Physica A, [DOI 10.1016/S0378-4371(97)00363-4](https://doi.org/10.1016/S0378-4371(97)00363-4)
  - Weierstraß-Mandelbrot signals with variable fractal dimension, Falconer, Fractal Geometry, Wiley, 2014, 3rd Ed., ISBN: 978-1-119-94239-9
  - Binary Cantor dusts with variable fractal dimension
- Optionally generates a plot display of signals
- Table can be exported as comma delimited text file in Fiji
- Note: Select to export column and row headers

### Cut out
- Cutting out of a sub-signal
- The range of the sub-signal is selected with the data value indices
- A range outside of the original signal is filled up with NaNs
- Signals should be opened with the CSAJ Signal Opener

### Resampling
- Down- or Upsampling
- A resampling factor can be set
- With or without linear interpolation
- Signals should be opened with the CSAJ Signal Opener

### Mathematics
- Computes mathematical functions
- Differentiation, Integration, Exp, Ln, Log, Sin, Cos, Tan
- Unity domain or optionally the 1st column as domain for differentiation and integration
- Data values of the 1st column as domain must be monotonically increasing
- Signals should be opened with the CSAJ Signal Opener
- Optional surrogate data conversion

### Filter
- Computes Moving average or Moving Median
- The range of average or median computation can be set
- The range must be an odd number
- A range of e.g. 3 means that for a data point, the previous value, the value itself and the next value are taken for the computation  
- Signals should be opened with the CSAJ Signal Opener
- Optional surrogate data conversion

### Noise
- Adding noise
- Shot, Salt&Pepper
  - Percentage of data points that will be changed can be set
- Uniform
  - Percentage of maximum value changes can be set
- Gaussian, Rayleigh, Exponential
  - Scaling parameter can be set

### Surrogates
- Computes surrogate signals
- Shuffle, Gaussian, Random phase, AAFT
- FFT windowing can be set
- Signals should be opened with the CSAJ Signal Opener
- Ref: Mark Shelhammer, Nonlinear Dynamics in Physiology, World Scientific 2007

### FFT
- Computes the Power- or Magnitude spectrum
- Several windowing functions can be set (default - Hanning)
- Normalization can be set to Standard or Unitary
- Output value scaling can be set to Log, Ln or Linear
- Time domain axis can be set to unitary units or to Hz 
- Sample frequency can be set for time domain in Hz
- Signals should be opened with the CSAJ Signal Opener
- Optional surrogate data conversion

### Autocorrelation
- Computes the autocorrelation
- Options for large and small number of data points
- FFT according to Wiener-Khinchin theorem
- The maximal lag can be set
- Signals should be opened with the CSAJ Signal Opener
- Optional surrogate data conversion
- Ref.: Oppenheim & Schafer, Discrete-Time Signal Processing, Ed.3, Pearson, 2010 

### Statistics
- Computes descriptive statistics
- N, Min, Max, Median, RMS, Mean, SD, Kurtosis, Skewness, Sum, Sum of squares
- Signals should be opened with the CSAJ Signal Opener
- Optional surrogate data analysis
- Analysis of Entire signal or Subsequent/Gliding boxes
- Notes for Subsequent/Gliding box:
  - Only Medians, Means and SDs are computed
  - The number of subsequent boxes is (signal length)/(box size)
  - The number of gliding boxes is (signal length)-(box size)
  - Note: The number of subsequent and particularly of gliding boxes can be very high

### Event detection
- Detects events such as peaks, valleys, slopes, or QRS peaks
- Output can be the event time, event value, interval, height, energy, or delta height
- Threshold or  Moving Average Curves (MACs)
- QRS detection based on the Chen&Chen or OSEA algorithm
- Optionally, the first data column may be the domain axis
- Optional surrogate data conversion
- Optional subtraction of the mean
- Optional up- or downscaling
- Signals should be opened with the CSAJ Signal Opener
- The osea-4-java algorithm/library developed by Patrick S. Hamilton from EP Limited is implemented
- See [MEDEVIT/OSEA-4-Java](https://github.com/MEDEVIT/OSEA-4-Java)
- Ref.: Lu et al., 2006, Medical Physics, [DOI 10.1118/1.2348764](https://doi.org/10.1118/1.2348764), Chen & Chen, 2003, Computers in Cardiology, [DOI 10.1109/CIC.2003.1291223](https://doi.org/10.1109/CIC.2003.1291223),
 Hamilton & Tompkins, 1987, IEEE Trans.Biomed.Eng., [DOI 10.1109/TBME.1986.325695](https://doi.org/10.1109/TBME.1986.325695)

### QRS peak detection (from file)
- Detects QRS peaks and peak to peak intervals of ECG data
- Plugin expects ECG files (Holter chan.raw files) from disk as input 
- Output is directly saved into a csv file as a table 
- Based on the OSEA algorithm
- The osea-4-java algorithm/library developed by Patrick S. Hamilton from EP Limited is implemented
- See [MEDEVIT/OSEA-4-Java](https://github.com/MEDEVIT/OSEA-4-Java)
- Ref.: Hamilton & Tompkins, 1987, IEEE Trans.Biomed.Eng., [DOI 10.1109/TBME.1986.325695](https://doi.org/10.1109/TBME.1986.325695)

### Standard HRV measurements
- Computes standard HRV measurements (e.g. for 24h ECG Holter recordings)
- Time domain and Frquency domain methods
- Several windowing functions for the frequency domain measurements (default - Hanning)
- MeanHR, MeanNN, SDNN, SDANN, SDNNI, HRVTI, RMSSD, SDSD, NN50, PNN50, NN20, PNN20, ULF, VLF, LF, HF, LFnorm, HFnorm, LF/HF, TP
- A time domain column is not needed, because it will be reconstructed by summing up subsequent beat to beat intervals
- Signals should be opened with the CSAJ Signal Opener
- Optional surrogate data analysis
- Analysis of Entire signal or Subsequent/Gliding boxes
- Notes for Subsequent/Gliding box:
  - One of the measurement parameters must be selected
  - The number of subsequent boxes is (signal length)/(box size)
  - The number of gliding boxes is (signal length)-(box size)
  - Note: The number of subsequent and particularly of gliding boxes can be very high
- Ref.: Malik et al., 1996, Circulation, [DOI 10.1161/01.CIR.93.5.1043](https://doi.org/10.1161/01.CIR.93.5.1043)

### Poincare plot
- Generation of Poincare plots (lag plots)
- The time lag can be set
- Multiple plots with distinct colors
- Signals should be opened with the CSAJ Signal Opener

### Detrended fluctuation analysis
- Computes Detrended fluctuation analysis.
- Signals should be opened with the CSAJ Signal Opener
- Optional surrogate data analysis
- Analysis of Entire signal or Subsequent/Gliding boxes
- Note for Entire signal:
  - Maximal window size should not be larger than 1/3 of the signal length
- Notes for Subsequent/Gliding box:
  - Maximal window size should not be larger than 1/3 of the box size
  - the box size should not be larger than 1/3 of the signal length 
  - The number of subsequent boxes is (signal length)/(box size)
  - The number of gliding boxes is (signal length)-(box size)
  - Note: The number of subsequent and particularly of gliding boxes can be very high
- Ref.: Peng et al., 1994, Phys.Rev.E., [DOI 10.1103/physreve.49.1685](https://doi.org/10.1103/physreve.49.1685)

### Hurst coefficient
- Computes the Hurst coefficient
- PSD - power spectrum density
- lowPSDwe - PSD using only lower frequencies (low), parabolic windowing (w) and end matching (e) (bridge detrending)
- for PSD, regression Min and Max can be set
- for lowPSDwe, regression Min = 1/8 and Max = 1/2 of the PSD size(1/2 of the signal length)
- SSC - signal summation conversion method for discriminating fGn from fBm signals
- Disp - dispersional method
- SWV - scaled windowed variance (mean of SD's)
- bdSWV - scaled windowed variance (mean of SD's) with bridge detrending
- Signals should be opened with the CSAJ Signal Opener
- Surrogate analysis is restricted to PSD Beta
- Analysis of Entire signal or Subsequent/Gliding box
- Notes for Subsequent/Gliding box:
  - Restricted to PSD Beta
  - The box size should not be larger than 1/3 of the signal length 
  - The number of subsequent boxes is (signal length)/(box size)
  - The number of gliding boxes is (signal length)-(box size)
  - Note: The number of subsequent and particularly of gliding boxes can be very high
- Ref.: Eke et al., 2000, Pflugers Archiv-European Journal of Physiology, [DOI 10.1007/s004249900135](https://doi.org/10.1007/s004249900135)

### Higuchi dimension
- Computes Higuchi dimensions
- Signals should be opened with the CSAJ Signal Opener
- Optional surrogate data analysis
- Analysis of Entire signal or Subsequent/Gliding boxes
- Note for Entire signal:
  - kMax should not be larger than 1/3 of the signal length
- Notes for Subsequent/Gliding box:
  - kMax should not be larger than 1/3 of the box size
  - The box size should not be larger than 1/3 of the signal length 
  - The number of subsequent boxes is (signal length)/(box size)
  - The number of gliding boxes is (signal length)-(box size)
  - Note: The number of subsequent and particularly of gliding boxes can be very high
- Ref.: Higuchi, 1988, Physica D, [DOI 10.1016/0167-2789(88)90081-4](https://doi.org/10.1016/0167-2789(88)90081-4)

### Tug of war dimension
- Computes Tug of war dimensions
- Binary [0, >0] algorithm!
- The number of boxes with distinct sizes according to the power of 2 can be set
- Linear regression parameters of the double log plot can be set 
- The ToW algorithm is a statistical approach and is dependent on the accuracy and confidence settings
- In the original paper accuracy=30 and confidence=5  
- But it is recommended to set accuracy and confidence as high as computation times allow
- Signals should be opened with the CSAJ Signal Opener
- Optional surrogate data analysis
- Analysis of Entire signal or Subsequent/Gliding boxes
- Note for Entire signal:
  - Number of Boxes should not be larger than 1/3 of the signal length
- Notes for Subsequent/Gliding box:
  - Number of boxes should not be larger than 1/3 of the box size
  - The box size should not be larger than 1/3 of the signal length 
  - The number of subsequent boxes is (signal length)/(box size)
  - The number of gliding boxes is (signal length)-(box size)
  - Note: The number of subsequent and particularly of gliding boxes can be very high
- Ref.: Reiss et al., Chaos, 2016, [DOI 10.1063/1.4966539](https://doi.org/10.1063/1.4966539)

### Katz dimension
- Computes Katz dimensions
- Signal lengths are computed by Euclidean distances 
- Signals should be opened with the CSAJ Signal Opener
- Optional surrogate data analysis
- Analysis of Entire signal or Subsequent/Gliding boxes
- Notes for Subsequent/Gliding box:
  - The box size should not be larger than 1/3 of the signal length 
  - The number of subsequent boxes is (signal length)/(box size)
  - The number of gliding boxes is (signal length)-(box size)
  - Note: The number of subsequent and particularly of gliding boxes can be very high
- Ref.: Katz, 1988, Comp Biol Med, [DOI 10.1016/0010-4825(88)90041-8](https://doi.org/10.1016/0010-4825(88)90041-8)

### Petrosian dimension
- Computes Petrosian dimensions
- Construction of a binary signal sequence by following a criterion
- Possible criterions:
- Greater or smaller than the mean of signal
- Greater or smaller than the mean+-SD range of signal
- Positive or negative sign of subsequent difference
- Greater or smaller than the mean of differences +-SD range of signal
- Signals should be opened with the CSAJ Signal Opener
- Optional surrogate data analysis
- Analysis of Entire signal or Subsequent/Gliding boxes
- Notes for Subsequent/Gliding box:
  - The box size should not be larger than 1/3 of the signal length 
  - The number of subsequent boxes is (signal length)/(box size)
  - The number of gliding boxes is (signal length)-(box size)
  - Note: The number of subsequent and particularly of gliding boxes can be very high
- Ref.: Petrosian, 1988, Proc 8th IEEE Symp Comp Med Syst, [DOI 10.1109/CBMS.1995.465426](https://doi.org/10.1109/CBMS.1995.465426)

### Sevcik dimension
- Computes Sevcik dimensions
- Gives better results than Katz dimension
- Signal lengths are computed by Euclidean distances 
- Signals should be opened with the CSAJ Signal Opener
- Optional surrogate data analysis
- Analysis of Entire signal or Subsequent/Gliding boxes
- Notes for Subsequent/Gliding box:
  - The box size should not be larger than 1/3 of the signal length 
  - The number of subsequent boxes is (signal length)/(box size)
  - The number of gliding boxes is (signal length)-(box size)
  - Note: The number of subsequent and particularly of gliding boxes can be very high
- Ref.: Sevcik, 1988, Complexity International, [arxiv.org/abs/1003.5266](http://arxiv.org/abs/1003.5266)

### RSE dimension (Roughness dimension)
- Computes Roughness scaling extraction dimensions
- M number of randomly chosen sub-sequences for each length (M=50 recommended)
- Scaling factor = 1 (fixed) 
- Flattening (detrending) of sub-sequences with polynomials
- Order of polynomials can be set
- 1st order is recommended (smallest error)
- 0th order is without flattening
- With increasing order, regression minimum should also be increased
- Signals should be opened with the CSAJ Signal Opener
- Optional surrogate data analysis
- Analysis of Entire signal or Subsequent/Gliding boxes
- Notes for Subsequent/Gliding box:
  - The box size should not be larger than 1/3 of the signal length 
  - The number of subsequent boxes is (signal length)/(box size)
  - The number of gliding boxes is (signal length)-(box size)
  - Note: The number of subsequent and particularly of gliding boxes can be very high
- Ref.: Wang et al., 2019, IEEE Access, [DOI 10.1109/ACCESS.2019.2926515](https://doi.org/10.1109/ACCESS.2019.2926515)


### Sample entropy
- Computes Sample or Approximate entropy
- Approximate entropy is not recommended for different signal lengths
- m length of subsignals (m=2 is often used)
- r maximal distance radius (0.1SD < r < 0.25SD, with SD the standard deviation of the time series)
- d additional delay according to Govindan et.al., 2007, PhysicaA, [DOI 10.1016/j.physa.2006.10.077](https://doi.org/10.1016/j.physa.2006.10.077)
- Signals should be opened with the CSAJ Signal Opener
- Optional surrogate data analysis
- Analysis of Entire signal or Subsequent/Gliding boxes
- Notes for Subsequent/Gliding box:
  - m should not be larger than 1/3 of the box size
  - The box size should not be larger than 1/3 of the signal length 
  - The number of subsequent boxes is (signal length)/(box size)
  - The number of gliding boxes is (signal length)-(box size)
  - Note: The number of subsequent and particularly of gliding boxes can be very high
- Ref.: Richman & Moorman, 2000, Am.J.Physiol.Heart.Circ.Physiol., [DOI 10.1152/ajpheart.2000.278.6.H2039](https://doi.org/10.1152/ajpheart.2000.278.6.H2039)

### Permutation entropy
- Computes Permutation entropies
- Permutation entropy H(n), Permutation entropy per symbol h(n)=H(n)/(n-1), Normalized permutation entropy H(n)/log(n!), Sorting entropy d(n)=H(n)-H(n-1), d(2)=H(2)  
- n Order of Permutation entropy (>=2)
- d Additional delay according to Govindan et.al., 2007, PhysicaA, [DOI 10.1016/j.physa.2006.10.077](https://doi.org/10.1016/j.physa.2006.10.077)
- Signals should be opened with the CSAJ Signal Opener
- Optional surrogate data analysis
- Analysis of Entire signal or Subsequent/Gliding boxes
- Notes for Subsequent/Gliding box:
  - n should not be larger than 1/3 of the box size
  - The box size should not be larger than 1/3 of the signal length 
  - The number of subsequent boxes is (signal length)/(box size)
  - The number of gliding boxes is (signal length)-(box size)
  - Note: The number of subsequent and particularly of gliding boxes can be very high
- Ref.: Bandt&Pompe, 2002, Phys.Rev.Lett, [DOI 10.1103/PhysRevLett.88.174102](https://doi.org/10.1103/PhysRevLett.88.174102)

### Generalised entropies
- Computes generalised entropies
- SE, H1, H2, H3, Renyi, Tsallis, SNorm, SEscort, SEta, SKappa, SB, SBeta, SGamma
- Probabilities are computed with plain signal values or with differences
- The time lag can be set
- Signals should be opened with the CSAJ Signal Opener
- Surrogate analysis is restricted to one of the entropies, if needed taking the smallest parameter value
- Analysis of Entire signal or Subsequent/Gliding box
- Notes for Subsequent/Gliding box:
  - Restricted to one of the entropies, if needed taking the smallest parameter value
  - The box size should not be larger than 1/3 of the signal length 
  - The number of subsequent boxes is (signal length)/(box size)
  - The number of gliding boxes is (signal length)-(box size)
  - Note: The number of subsequent and particularly of gliding boxes can be very high
- Ref.: Amigo et al., 2018, Entropy, [DOI 10.3390/e20110813](https://doi.org/10.3390/e201108), Tsallis, Introduction to Nonextensive
Statistical Mechanics, Springer, 2009

### Kolmogorov complexity and Logical depth
- KC is estiamted by compressing data bytes (ZLIB, GZIB)
- LD is estimated by the decompression time
- Iterations should be set to as high a value as possible
- LD values should be taken with caution, as computers are not well suited to measure times
- Signals should be opened with the CSAJ Signal Opener
- Optional surrogate data analysis
- Analysis of Entire signal or Subsequent/Gliding boxes
- Notes for Subsequent/Gliding box:
  - the box size should not be larger than 1/3 of the signal length 
  - The number of subsequent boxes is (signal length)/(box size)
  - The number of gliding boxes is (signal length)-(box size)
  - Note: The number of subsequent and particularly of gliding boxes can be very high
- Ref.: Zenil et al., 2012, Complexity, [DOI 10.1002/cplx.20388](https://doi.org/10.1002/cplx.20388)

### Lyapunov exponent
- Computes the largest Lyapunov exponent
- Rosenstein's and Kantz's algorithms are implemented
- Additionally a direct method without phase space reconstruction is implemented but should be used with care.
- The maximum delay can be set
- The regression minimum and maximum can be set
- The most linear part of the first k values should be taken
- The embedding dimension can be set (usually not too high)
- The tau for the pase space reconstruction can be set (a relaible value value is the first minimum of the autocorrelation)
- The sampling frequency can be set. If not known it should be 1
- For the Rosenstein algorithm only one initial distance (minimum distance) is used.
- For the Kantz algorithm the number of initial distances can be set. The average of the linear regressions is computed. This might be better for noisy data series. 
- For the Direct method a maximum eps (initial distance) can be set. Averages for every k are computed.
- Signals should be opened with the CSAJ Signal Opener
- Optional surrogate data analysis
- Analysis of Entire signal or Subsequent/Gliding boxes
- Notes for Subsequent/Gliding box:
  - the box size should not be larger than 1/3 of the signal length 
  - The number of subsequent boxes is (signal length)/(box size)
  - The number of gliding boxes is (signal length)-(box size)
  - Note: The number of subsequent and particularly of gliding boxes can be very high
- Ref.: Rosenstein et al., 1993, PhysicaD, [10.1016/0167-2789(93)90009-P](https://doi.org/10.1016/0167-2789(93)90009-P), Kantz, 1994, Physics Letters A, [10.1016/0375-9601(94)90991-1](https://doi.org/10.1016/0375-9601(94)90991-1)

### Allometric scaling
- Computes a double log plot of aggregated variances and means
- Regression parameters can be set
- The slope of the linear regression is the result
- According to West: FD = 2 - slope/2, but this does not always yield reasonable values
- Signals should be opened with the CSAJ Signal Opener
- Optional surrogate data analysis
- Analysis of Entire signal or Subsequent/Gliding boxes
- Note for Entire signal:
  - Regression Max should not be larger than 1/3 of the signal length
- Notes for Subsequent/Gliding box:
  - Regression Max should not be larger than 1/3 of the box size
  - the box size should not be larger than 1/3 of the signal length 
  - The number of subsequent boxes is (signal length)/(box size)
  - The number of gliding boxes is (signal length)-(box size)
  - Note: The number of subsequent and particularly of gliding boxes can be very high
- Ref.: West, 2006, Complexity, [DOI 10.1002/cplx.20114](https://doi.org/10.1002/cplx.20114)

### Symbolic aggregation
- Generates symbolic aggregation
- Output is an image
- Aggregation size = Sqrt(Alphabet size) x 2^(Sub-word length - 1)
- Aggregation length can be set
- Alphabet size is fixed to 4 characters (may be expanded later)
- Word length can be set
- Sub-word length can be set
- Magnification of the aggregation size to an image size can be set
- Grey or color image(s)
- Optional surrogate data conversion
- Signals should be opened with the CSAJ Signal Opener
- Ref: Lin et al., 2003, DMKD '03: Proceedings of the 8th ACM SIGMOD workshop on Research issues in data mining and knowledge discovery [DOI 10.1145/882082.882086](https://doi.org/10.1145/882082.882086)
  

