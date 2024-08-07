# IF Filter for SSB 

** table of content **
- [IF Filter for SSB](#if-filter-for-ssb)
  - [Description](#description)
  - [Circuit](#circuit)
  - [Measuring Crystals](#measuring-crystals)

## Description 
A crystal ladder filter is an electronic filter that uses quartz crystals to achieve highly selective frequency filtering. It is used in this radio in the IF stage to filter out unwanted signals as it has a very narrow bandwidth of usually between 2kHz and 3kHz. 


## Circuit
<img src="img/WhatsApp Image 2024-07-19 at 13.43.12.jpeg" width=100%>
The crystals used are 16MHz and the capacitors are all 100pF to get a bandwidth of 2.7kHz with minimal insertion loss of less than 3dB. <br><br>

<img src="img/WhatsApp Image 2024-07-19 at 13.43.13.jpeg" width=100%>
In this image you can see the filter response on the NanoVNA. The center frequency was 15.995 000 MHz and the frequency span was 10 kHz. 


## Measuring Crystals
<img src="img/WhatsApp Image 2024-07-18 at 14.02.20.jpeg" width=50%><img src="img/WhatsApp Image 2024-07-18 at 14.02.20 (1).jpeg" width=50%>
This is the measuring jig that i build where you just put in the crystal and connect both coax cables to the left and right side then you measure the S21 on for example the NanoVNA (seen in the picture on the right). 
<br>


<img src="img/WhatsApp Image 2024-07-18 at 14.03.02-new.jpeg" width=100%>
Here you can see the coarsely measured crystals in the respected groups of frequency. The next step is to measure using a lower frequency span and groupe them in 100Hz increments (dFmax = BW / 100 = 270Hz). 
<br><br>

<img src="img/WhatsApp Image 2024-07-18 at 14.03.02 (1).jpeg" width=100%>
In this image you can see said measurements grouped in 100Hz increments. 