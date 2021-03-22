## Signal - Short description of plugins

### Autocorrelation
- Computes the autocorrelation
- Options for large and small number of data points
- FFT according to Wiener-Khinchin theorem
- The maximal lag can be set
- Signals should be opened with the CSAJ Signal Opener
- Optional surrogate data conversion
- Ref.: Oppenheim & Schafer, Discrete-Time Signal Processing, Ed.3, Pearson, 2010 

### Cut out
- Cutting out of a sub-signal
- The range of the sub-signal is selected with the data value indices
- A range outside of the original signal is filled up with NaNs
- Signals should be opened with the CSAJ Signal Opener

### Detect QRS peaks
- Detects QRS peaks and peak to peak intervals of ECG data
- Based on the OSEA algorithm
- For ECG files (Holter chan.raw files)
- Signals should be opened with the CSAJ Signal Opener
- The osea-4-java algorithm/library developed by Patrick S. Hamilton from EP Limited is implemented
- See [https://github.com/MEDEVIT/OSEA-4-Java](https://github.com/MEDEVIT/OSEA-4-Java)
- Ref.: Hamilton & Tompkins, 1987, IEEE Trans.Biomed.Eng., [https://doi.org/10.1109/TBME.1986.325695](https://doi.org/10.1109/TBME.1986.325695)

### Detrended fluctuation analysis
- Computes Detrended fluctuation analysis.
- Signals should be opened with the CSAJ Signal Opener
- Optional surrogate data analysis
- Evaluation of Entire signal or Subsequent/Gliding boxes
- Note for Entire signal:
  - Maximal window size should not be larger than 1/3 of the signal length
- Notes for Subsequent/Gliding box:
  - Maximal window size should not be larger than 1/3 of the box size
  - the box size should not be larger than 1/3 of the signal length 
  - The number of subsequent boxes is (signal length)/(box size)
  - The number of gliding boxes is (signal length)-(box size)
  - Note: The number of subsequent and particularly of gliding boxes can be very high
- Ref.: Peng et al., 1994, Phys.Rev.E., [https://doi.org/10.1103/physreve.49.1685](https://doi.org/10.1103/physreve.49.1685)

### Fractal dimension - Higuchi
- Computes Higuchi dimensions
- Signals should be opened with the CSAJ Signal Opener
- Optional surrogate data analysis
- Evaluation of Entire signal or Subsequent/Gliding boxes
- Note for Entire signal:
  - kMax should not be larger than 1/3 of the signal length
- Notes for Subsequent/Gliding box:
  - kMax should not be larger than 1/3 of the box size
  - The box size should not be larger than 1/3 of the signal length 
  - The number of subsequent boxes is (signal length)/(box size)
  - The number of gliding boxes is (signal length)-(box size)
  - Note: The number of subsequent and particularly of gliding boxes can be very high
- Ref.: Higuchi, 1988, Physica D, [https://doi.org/10.1016/0167-2789(88)90081-4](https://doi.org/10.1016/0167-2789(88)90081-4)

### Generalised entropies
- Computes generalised entropies
- SE, H1, H2, H3, Renyi, Tsallis, SNorm, SEscort, SEta, SKappa, SB, SBeta, SGamma
- Probabilities are computed with actual signal values or with differences
- The time lag can be set
- Signals should be opened with the CSAJ Signal Opener
- Surrogate analysis is restricted to one of the entropies, if needed taking the smallest parameter value
- Evaluation of Entire signal or Subsequent/Gliding box
- Notes for Subsequent/Gliding box:
  - Restricted to one of the entropies, if needed taking the smallest parameter value
  - The box size should not be larger than 1/3 of the signal length 
  - The number of subsequent boxes is (signal length)/(box size)
  - The number of gliding boxes is (signal length)-(box size)
  - Note: The number of subsequent and particularly of gliding boxes can be very high
- Ref.: Amigo et al., 2018, Entropy, [https://doi.org/10.3390/e20110813](https://doi.org/10.3390/e201108), Tsallis, Introduction to Nonextensive
Statistical Mechanics, Springer, 2009

### Kolmogorov complexity and Logical depth
- KC is estiamted by compressing data bytes (ZLIB, GZIB)
- LD is estimated by the decompression time
- Iterations should be set to as high a value as possible
- LD values should be taken with caution, as computers are not well suited to measure times
- Signals should be opened with the CSAJ Signal Opener
- Optional surrogate data analysis
- Ref.: Zenil et al., 2012, Complexity, [https://doi.org/10.1002/cplx.20388](https://doi.org/10.1002/cplx.20388)

### Mathematical functions
- Computes mathematical functions
- Differentiation, Integration, Exp, Ln, Log, Sin, Cos, Tan
- Unity domain or optionally the 1st column as domain for differentiation and integration
- Data values of the 1st column as domain must be monotonically increasing
- Signals should be opened with the CSAJ Signal Opener
- Optional surrogate data conversion

### Poincare plot
- Generation of Poincare plots (lag plots)
- The time lag can be set
- Multiple plots with distinct colors

### Sample entropy
- Computes Sample or Approximate entropy
- Signals should be opened with the CSAJ Signal Opener
- Note: Approximate entropy is not recommended for different signal lengths
- m length of subsignals (m=2 is often used)
- r maximal distance radius (0.1SD < r < 0.25SD, with SD the standard deviation of the time series)
- d additional delay according to Govindan et.al., 2007, PhysicaA, [https://doi.org/10.1016/j.physa.2006.10.077](https://doi.org/10.1016/j.physa.2006.10.077)
- Optional surrogate data analysis
- Evaluation of Entire signal or Subsequent/Gliding boxes
- Notes for Subsequent/Gliding box:
  - m should not be larger than 1/3 of the box size
  - The box size should not be larger than 1/3 of the signal length 
  - The number of subsequent boxes is (signal length)/(box size)
  - The number of gliding boxes is (signal length)-(box size)
  - Note: The number of subsequent and particularly of gliding boxes can be very high
- Ref.: Richman & Moorman, 2000, Am.J.Physiol.Heart.Circ.Physiol., [https://doi.org/10.1152/ajpheart.2000.278.6.H2039](https://doi.org/10.1152/ajpheart.2000.278.6.H2039)

### Signal generator
- Generates signals of floating values (dot decimal numbers)
- Generates a table for further processing
- Table can be exported as comma delimited text file
- Optionally generates a plot display of signals
- Following signal types are supported:
  - Constant, Sine, Square, Triangle, Sawtooth
  - Gaussian an Uniform noise
  - Discrete chaotical maps (Logistic, Henon, Cubic, Spence), Silva & Murta Jr., 2012, Chaos, [http://dx.doi.org/10.1063/1.4758815](http://dx.doi.org/10.1063/1.4758815) 
  - Fractional Gaussian noise signals depending on the Hurst coefficient and using Davis and Harte autocorrelation method DHM
  - Fractional Gaussian motion signals depending on the Hurst coefficient and using spectral synthesis method SSM, Eke et al., 2000, Pflugers Archiv-European Journal of Physiology, [https://doi.org/10.1007/s004249900135](https://doi.org/10.1007/s004249900135) Caccia et.al., 1997, Physica A, [https://doi.org/10.1016/S0378-4371(97)00363-4](https://doi.org/10.1016/S0378-4371(97)00363-4)

### Signal opener
- Opens signals or plots of floating values (dot decimal numbers)
- For comma delimited text files
- The first row must contain the column (signal) headers
- The first column must contain the row headers or just a subsequent numbering 
- All columns MUST have the same number of rows
- Maximal row number is 2147483647 or 2^31-1
- Missing values MUST be filled up with NaN
- NaNs will be ignored for plot charts
- NaNs will be ignored by CSAJ signal plugins
- Conversion of integer to floating numbers with Notepad++ can be found [here](notepadpp/IntegerToFloating.md) 
- Filling up missing values by NaNs with Notepad++ can be found [here](notepadpp/FillNaNs.md) 

### Standard HRV measurements
- Computes standard HRV measurements (e.g. for 24h ECG Holter recordings)
- Time domain and Frquency domain methods
- MeanHR, MeanNN, SDNN, SDANN, SDNNI, HRVTI, RMSSD, SDSD, NN50, PNN50, NN20, PNN20, VLF, LF, HF, LFnorm, HFnorm, LF/HF, TP
- Signals should be opened with the CSAJ Signal Opener
- A time domain column is not needed, because it will be reconstructed by summing up subsequent beat to beat intervals
- Optional surrogate data analysis
- Evaluation of Entire signal or Subsequent/Gliding boxes
- Notes for Subsequent/Gliding box:
  - One of the measurement parameters must be selected
  - The number of subsequent boxes is (signal length)/(box size)
  - The number of gliding boxes is (signal length)-(box size)
  - Note: The number of subsequent and particularly of gliding boxes can be very high
- Ref.: Malik et al., 1996, Circulation, [https://doi.org/10.1161/01.CIR.93.5.1043](https://doi.org/10.1161/01.CIR.93.5.1043)

### Statistics
- Computes descriptive statistics
- N, Min, Max, Median, RMS, Mean, SD, Kurtosis, Skewness, Sum, Sum of squares
- Signals should be opened with the CSAJ Signal Opener
- Optional surrogate data analysis
- Evaluation of Entire signal or Subsequent/Gliding boxes
- Notes for Subsequent/Gliding box:
  - Only Medians, Means and SDs are computed
  - The number of subsequent boxes is (signal length)/(box size)
  - The number of gliding boxes is (signal length)-(box size)
  - Note: The number of subsequent and particularly of gliding boxes can be very high
  

