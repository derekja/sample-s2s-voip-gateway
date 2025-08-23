# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Building and Running
- **Build project**: `mvn package` - Compiles Java code and creates executable JAR in target/
- **Quick run (local development)**: `./run.sh` - Compiles and runs main class directly via Maven exec plugin
- **Build Docker image**: `make build` - Builds Maven project and creates Docker image
- **Run with Docker**: `make run` (foreground) or `make run-bg` (background) - Requires `environment` file
- **Build without Docker**: `mvn compile` - Compiles source without packaging

### CDK Deployment
Two deployment options are available:

**ECS Deployment (Production-like)**:
```bash
cd cdk-ecs
npm install
cdk bootstrap  # First time only
cdk deploy
cdk destroy    # Cleanup
```

**EC2 Deployment (Development/Testing)**:
```bash
cd cdk-ec2-instance
npm install
cdk bootstrap  # First time only
cdk deploy
cdk destroy    # Cleanup
```

## Project Architecture

### Core Components
- **Main Entry Point**: `NovaSonicVoipGateway.java` - Extends `RegisteringMultipleUAS`, handles SIP registration and call management
- **Nova Integration**: `NovaStreamerFactory.java` - Creates media streamers that bridge RTP audio with AWS Bedrock Nova Sonic
- **Media Configuration**: `NovaMediaConfig.java` - Configures Nova voice, prompt, and audio settings
- **Audio Processing**: 
  - `NovaSonicAudioInput/Output.java` - Handle audio I/O between RTP and Nova
  - `PcmToULawTranscoder.java` / `UlawToPcmTranscoder.java` - Audio format conversion
  - `VoiceActivityDetector.java` - Detects speech for barge-in functionality

### Event System
- **Event-driven architecture** using observer pattern
- **Event types**: `AudioInputEvent`, `ContentStartEvent`, `ContentEndEvent`, `PromptStartEvent`, etc.
- **Event handlers**: `NovaS2SEventHandler.java` processes Nova Sonic events
- **Observers**: `InputEventsInteractObserver.java` handles interaction events

### Tool System
Nova Sonic can be extended with tools in `nova/tools/`:
- **Base class**: `AbstractNovaS2SEventHandler.java` - Extend this for new tools
- **Example tool**: `DateTimeNovaS2SEventHandler.java` - Provides date/time functionality
- **Tool registration**: Add new tools in `NovaStreamerFactory.createMediaStreamer()`

### Configuration
The application supports two configuration modes:
1. **Environment variables** (when `SIP_SERVER` is set)
2. **Configuration file** (`.mjsip-ua` file when `SIP_SERVER` not set)

Key environment variables: `AUTH_USER`, `AUTH_PASSWORD`, `SIP_SERVER`, `NOVA_VOICE_ID`, `NOVA_PROMPT`, `MEDIA_PORT_BASE`, `ENABLE_BARGE_IN`, `ENABLE_CONVERSATION_LOG`

### Dependencies
- **mjSIP**: SIP protocol implementation (GPLv2 licensed fork from GitHub)
- **AWS SDK**: Bedrock integration for Nova Sonic
- **Jackson**: JSON processing
- **RxJava/Reactor**: Reactive streaming
- **Lombok**: Code generation

### Maven Configuration
- **Java version**: 9+ (configured for Java 9 compatibility)
- **GitHub Maven repository**: Requires authentication token in `~/.m2/settings.xml` for mjSIP dependency
- **Shade plugin**: Creates fat JAR with all dependencies
- **Main class**: `com.example.s2s.voipgateway.NovaSonicVoipGateway`

### Infrastructure as Code
- **Two CDK stacks**: ECS-based (production) and EC2-based (development)
- **ECS stack**: Creates VPC, ECS cluster, auto-scaling, secrets management, ECR integration
- **EC2 stack**: Simple single-instance deployment with proper IAM roles and security groups
- **Networking requirements**: UDP ports 5060 (SIP) and 10000-20000 (RTP) must be open

### Audio and Media
- **Audio format**: PCM 16-bit, 8kHz sample rate for Nova; μ-law for RTP
- **Transcoding**: Automatic conversion between PCM and μ-law formats
- **Voice Activity Detection**: Enables natural conversation flow and barge-in
- **Media streaming**: Bridges RTP (VoIP) and Nova Sonic audio streams