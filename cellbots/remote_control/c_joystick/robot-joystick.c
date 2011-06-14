// Released into the public domain, 5 May 2010
// Google, inc. Jason E. Holt <jholt [at] google.com>
//
// Sample code to read events from a Linux joystick device and translate
// them into commands for a two-wheel differential drive robot.
//
// If you're just looking for code to read your /dev/input/js0, it's
// pretty much all in main.  The rest is specific to our robot.

#include <linux/joystick.h>
#include <sys/ioctl.h>
#include <error.h>
#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdlib.h>
#include <string.h>
#include <linux/input.h>
#include <unistd.h>
#include <stdarg.h>
#include <pthread.h>
#include <math.h>

#define COMMAND_DELAY (100000)
#define LEFT_GAIN (1.3)
#define RIGHT_CAP (180)

void serial_command(char *format, ...) {
  va_list ap;

  va_start(ap, format);

  vfprintf(stdout, format, ap);
  fprintf(stdout, "\r\n");
  fflush(stdout);

  vfprintf(stderr, format, ap);
  fprintf(stderr, "\n");

  usleep(COMMAND_DELAY);

  va_end(ap);
}

int pitch_global=0, yaw_global=0;

void actuate_wheels(int pitch, int yaw) {

  //printf("pitch: %d yaw: %d\n", pitch, yaw);

  // scale to -100:100
  float pitch_float = pitch / 327.68;
  float yaw_float = yaw / 327.68;
 
  //printf("pitch: %f yaw: %f\n", pitch_float, yaw_float);

  // increase speed quadratically, to improve low-speed control
  //pitch_float = 100.0 * (pitch_float / 100.0)* (pitch_float / 100.0)
  //yaw_float = 100.0 * (yaw_float / 100.0)* (yaw_float / 100.0)

  // clamp
  if (pitch_float > 100.0) pitch_float = 100.0;
  if (pitch_float < -100.0) pitch_float = -100.0;
  if (yaw_float > 100.0) yaw_float = 100.0;
  if (yaw_float < -100.0) yaw_float = -100.0;
 
  // rotate into wheel space from pitch/yaw space
  float left_wheel = pitch_float + yaw_float;
  float right_wheel = pitch_float - yaw_float;

  //printf("left: %f right: %f\n", left_wheel, right_wheel);

  // if that pushed us over 100%, scale back without changing
  // direction
  float scale = 1.0;
  if (fabs(left_wheel) > 100 || fabs(right_wheel) > 100) {
    if (fabs(left_wheel) > fabs(right_wheel)) {
      scale = 100.0 / fabs(left_wheel);
    } else {
      scale = 100.0 / fabs(right_wheel);
    }
    left_wheel = scale * left_wheel;
    right_wheel = scale * right_wheel;
  }

  //printf("left: %f right: %f\n", left_wheel, right_wheel);

  // stop
  if (pitch == 0 && yaw == 0) {
    serial_command("s");
    return;
  }

  // calibrate out differences in the motors
  if (left_wheel > 0) {
    float a = 3.76589;
    float b = 0.77934;
    float c = -0.00367769;

    left_wheel = a + b*left_wheel + c*left_wheel*left_wheel;
  }

  if (right_wheel < 0.0) {
    float a = -4.677;
    float b = 0.5971769;
    float c = 0.00244537;

    right_wheel = a + b*right_wheel + c*right_wheel*right_wheel;
  }

  //printf("left: %f right: %f\n", left_wheel, right_wheel);

  int left = left_wheel;
  int right = right_wheel;

  serial_command("w %d %d", left, right);
}
 
void *wheel_loop(void *unused) {
  serial_command("s");
  while(1) {
    actuate_wheels(pitch_global, yaw_global);
//    actuate_wheels(255, 0);
  }
}

main(int argc, char **argv) {
  if (argc != 2) {
    printf("Usage: %s /dev/input/js0\n", argv[0]);
    return 1;
  }

  //int jd = open(argv[1], O_RDONLY);
  int jd = open(argv[1], O_RDONLY);
  if (jd == -1) {
    perror("Opening the file you specified:");
    return 2;
  }

  pthread_t wheel_thread;
  pthread_create(&wheel_thread, NULL, wheel_loop, NULL);

  while (1) {
    struct js_event event;
    read(jd, &event, sizeof(struct js_event));

    switch (event.type) {
      case JS_EVENT_BUTTON:
//        printf("Button %hhd = %hd\n", event.number, event.value);
        break;

      case JS_EVENT_AXIS:
//        printf("Axis %hhd = %hd\n", event.number, event.value);
          if (event.number == 1) {
            pitch_global = -event.value;
          } else if (event.number == 0) {
            yaw_global = event.value;
          }
        break;

      default:
        printf("Unknown joystick event\n");
        break;
    }
  }
}
