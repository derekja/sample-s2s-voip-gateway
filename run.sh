#!/bin/bash
set -e

mvn compile exec:java -e -Dexec.mainClass=com.example.s2s.voipgateway.NovaSonicVoipGateway

