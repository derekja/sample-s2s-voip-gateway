#!/bin/bash
set -e

mvn exec:java -Dexec.mainClass=com.example.s2s.voipgateway.NovaSonicVoipGateway

