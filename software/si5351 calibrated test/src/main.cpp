/*
 * si5351_calibration.ino - Simple calibration routine for the Si5351
 *                          breakout board.
 *
 * Copyright 2015 - 2018 Paul Warren <pwarren@pwarren.id.au>
 *                       Jason Milldrum <milldrum@gmail.com>
 *
 * Uses code from https://github.com/darksidelemm/open_radio_miniconf_2015
 * and the old version of the calibration sketch
 *
 * This sketch  is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License.
 * If not, see <http://www.gnu.org/licenses/>.
 */
#include <Arduino.h>
#include "si5351.h"
#include "Wire.h"

Si5351 si5351;

int32_t cal_factor = 155989;

uint64_t target_freq = 3000000000ULL; // 30 MHz, in hundredths of hertz

// #######################################################################################################################################

static void flush_input(void);
static void vfo_interface(void);

// #######################################################################################################################################

void setup()
{
  // Start serial and initialize the Si5351
  Serial.begin(115200);

  // The crystal load value needs to match in order to have an accurate calibration
  si5351.init(SI5351_CRYSTAL_LOAD_8PF, 0, 0);

  // Start on target frequency
  si5351.set_correction(cal_factor, SI5351_PLL_INPUT_XO);
  si5351.set_pll(SI5351_PLL_FIXED, SI5351_PLLA);
  si5351.drive_strength(SI5351_CLK0, SI5351_DRIVE_8MA);
  Serial.println("Setup");
  si5351.set_freq(target_freq, SI5351_CLK0);
  si5351.update_status();
}

void loop()
{
}
