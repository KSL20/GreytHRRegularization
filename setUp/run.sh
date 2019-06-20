#!/bin/sh
export DISPLAY=:0
cd ~/GreytHRRegularization
java -cp target/regularization-1.0.0.jar com.regularization.RegularizationService userId password > /tmp/greythr.logs
