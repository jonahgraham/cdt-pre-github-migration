################################################################################
# Automatically-generated file. Do not edit!
################################################################################

-include ../makefile.init

RM := rm -rf

# All of the sources participating in the build are defined here
-include sources.mk
-include dir1/dd/ff/subdir.mk
-include dir1/dd/excluded_c/asd/subdir.mk
-include dir1/dd/excluded_c/subdir.mk
-include d1_1/d2_1/subdir.mk
-include d1_1/subdir.mk
-include d1/d2/d3/subdir.mk
-include d1/d2/subdir.mk
-include d1/subdir.mk

ifneq ($(MAKECMDGOALS),clean)
ifneq ($(strip $(C++_DEPS)),)
-include $(C++_DEPS)
endif
ifneq ($(strip $(CC_DEPS)),)
-include $(CC_DEPS)
endif
ifneq ($(strip $(CPP_DEPS)),)
-include $(CPP_DEPS)
endif
ifneq ($(strip $(CXX_DEPS)),)
-include $(CXX_DEPS)
endif
ifneq ($(strip $(C_DEPS)),)
-include $(C_DEPS)
endif
ifneq ($(strip $(C_UPPER_DEPS)),)
-include $(C_UPPER_DEPS)
endif
endif

-include ../makefile.defs

OPTIONAL_TOOL_DEPS := \
$(wildcard ../makefile.defs) \
$(wildcard ../makefile.init) \
$(wildcard ../makefile.targets) \


BUILD_ARTIFACT_NAME := test_40
BUILD_ARTIFACT_EXTENSION :=
BUILD_ARTIFACT_PREFIX :=
BUILD_ARTIFACT := $(BUILD_ARTIFACT_PREFIX)$(BUILD_ARTIFACT_NAME)$(if $(BUILD_ARTIFACT_EXTENSION),.$(BUILD_ARTIFACT_EXTENSION),)

# Add inputs and outputs from these tool invocations to the build variables 

# All Target
all: main-build

# Main-build Target
main-build: test_40

# Tool invocations
test_40: $(OBJS) $(USER_OBJS) makefile $(OPTIONAL_TOOL_DEPS) uo2
	@echo 'Building target: $@'
	@echo 'Invoking: Test 4.0 ToolName.linker.gnu.cpp'
	g++ -Llib/path/2 -o "test_40" $(OBJS) $(USER_OBJS) $(LIBS) uo2 -llib2
	@echo 'Finished building target: $@'
	@echo ' '

# Other Targets
clean:
	-$(RM) test_40
	-@echo ' '

.PHONY: all clean dependents main-build

-include ../makefile.targets
