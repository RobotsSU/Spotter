
# Config download method (USB)
COMM=-U

# Path to the NXC compiler and downloader
NXC=$(NXC_PATH)/nbc
NXTCOM=$(NXC_PATH)/nxtcom

#Directory to store compiled files in
OUTPUT_DIRECTORY=build

# Options to pass to the compiler
OPTIONS=""

# The main file name to compile
PROGRAM=RobotCommand

# The name of the output file to generate
OUTPUT=RobotCommand

dummy := $(shell test -d $(OUTPUT_DIRECTORY) || mkdir -p $(OUTPUT_DIRECTORY))

all: $(OUTPUT).rxe download

$(OUTPUT).rxe: source/*.nxc Makefile
	cd source;                    \
	$(NXC) -O=../build/$(OUTPUT).rxe \
		        $(PROGRAM).nxc

download: $(OUTPUT).rxe
	cd build;						\
	$(NXTCOM) $(COMM) $(OUTPUT).rxe
	
clean:
	/bin/rm -vf build/$(OUTPUT).rxe
		   
