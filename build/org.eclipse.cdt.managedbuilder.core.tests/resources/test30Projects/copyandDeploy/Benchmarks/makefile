################################################################################
# Automatically-generated file. Do not edit!
################################################################################

-include ../makefile.init

RM := rm -rf

# All of the sources participating in the build are defined here
-include sources.mk
-include Functions/subdir.mk
-include subdir.mk

ifneq ($(MAKECMDGOALS),clean)
ifneq ($(strip $(C_DEPS)),)
-include $(C_DEPS)
endif
endif

-include ../makefile.defs

OPTIONAL_TOOL_DEPS := \
$(wildcard ../makefile.defs) \
$(wildcard ../makefile.init) \
$(wildcard ../makefile.targets) \


BUILD_ARTIFACT_NAME := TestCopyandDeploy
BUILD_ARTIFACT_EXTENSION :=
BUILD_ARTIFACT_PREFIX :=
BUILD_ARTIFACT := $(BUILD_ARTIFACT_PREFIX)$(BUILD_ARTIFACT_NAME)$(if $(BUILD_ARTIFACT_EXTENSION),.$(BUILD_ARTIFACT_EXTENSION),)

# Add inputs and outputs from these tool invocations to the build variables 
EXECUTABLES += \
USER_OBJS.foo \


# All Target
all: main-build

# Main-build Target
main-build: TestCopyandDeploy

# Tool invocations
TestCopyandDeploy: $(EXECUTABLES) makefile $(OPTIONAL_TOOL_DEPS)
	@echo 'Building target: $@'
	@echo 'Invoking: Deploy tool'
	cp $(EXECUTABLES) "TestCopyandDeploy"
	@echo 'Finished building target: $@'
	@echo ' '

USER_OBJS.foo: $(OBJS) $(USER_OBJS) makefile $(OPTIONAL_TOOL_DEPS)
	@echo 'Invoking: MBS30.linker.gnu.c'
	gcc  -o "USER_OBJS.foo" $(OBJS) $(USER_OBJS) $(LIBS)
	@echo 'Finished building: $@'
	@echo ' '

# Other Targets
clean:
	-$(RM) TestCopyandDeploy USER_OBJS.foo
	-@echo ' '

.PHONY: all clean dependents main-build

-include ../makefile.targets
