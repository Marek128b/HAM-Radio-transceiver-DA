# 20m (14MHz - 14.35MHz) BP Filter 

** table of content **
- [20m (14MHz - 14.35MHz) BP Filter](#20m-14mhz---1435mhz-bp-filter)
  - [Description](#description)
  - [Circuit](#circuit)
  - [How it looks like in Real life](#how-it-looks-like-in-real-life)

## Description 
Bandpass filters are used to effectively reduce strong signals outside the operating band. This reduces noise and in-band noise, in a receiver there is a much lower risk of producing unwanted signals in the mixer. This gives better overall reception. <br>
On the transmit side, spurious emission signals are suppressed as well, reducing interference with the neighbouring stations. 

[comment]: <https://www.wimo.com/en/12410-20> (copied text from here)


## Circuit
The Circuit was designed with the online 
[Filter Tool](https://markimicrowave.com/technical-resources/tools/lc-filter-design-tool/)
to have a lower Frequency of 13MHz and a rather high upper cutoff frequency of 17MHz. 
The Air coils where calculated using the [Coil 32 online Tool](https://coil32.net/online-calculators/one-layer-coil-calculator.html). The coils where wound by hand using Wrapping wire around a 3D-Printed plastic core with respected diameters and lengths. <br>
The 3D-designs are found in the `20m BP Filter/hardware` folder.

<br>
20m 3rd Order Chebyshev Filter 

<img src="img/Marki Filter 20m BO.PNG" width=100%>

<br>
Simulated using LT-Spice with theoretical practical values:

<img src="https://github.com/user-attachments/assets/99542045-f6d4-47c8-bed9-08124b4745b5" width=100%>

<br>
Measured with the NanoVNA:

config in NanoVNA Saver v0.6.4:

<img src="https://github.com/user-attachments/assets/3b711448-d48a-4637-ac91-fb85a58c8e81" width=auto>

S21 graph:

<img src="https://github.com/user-attachments/assets/ebafac80-e0cf-4957-a33c-2a3c810d1a0e" width=100%>

markers:

<img src="https://github.com/user-attachments/assets/124bea51-b577-424b-abfb-ee1652be12ea" width=auto>
 


## How it looks like in Real life
<img src="img/WhatsApp Image 2024-07-28 at 16.52.38.jpeg" width=100%>