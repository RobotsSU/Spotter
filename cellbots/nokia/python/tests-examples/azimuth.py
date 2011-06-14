from sensor import *
import e32, math 
direction = 1

class DemoApp(): 

    def __init__(self):

        self.magnetic_north = MagneticNorthData()

        self.magnetic_north.set_callback(data_callback=self.my_callback) 

    def my_callback(self):


        azimuth = self.magnetic_north.azimuth
	azimuthstr = str(azimuth)
        print "azimuth", azimuthstr 
        if  azimuth - direction > 180 and abs(azimuth - direction) > 10:
		print "LEFT"
        if azimuth - direction < 180 and abs(azimuth - direction) > 10:
		print "RIGHT"

    def run(self):

        self.magnetic_north.start_listening() 

if __name__ == '__main__':

    d = DemoApp()

    d.run()

    e32.ao_sleep(5000000)

    d.magnetic_north.stop_listening()

    print "Exiting MagneticNorth" 
