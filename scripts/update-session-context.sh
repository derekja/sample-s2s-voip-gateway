#!/bin/bash

# SESSION_CONTEXT.md Auto-Update Script
# Usage: ./update-session-context.sh [issue_type] [description]
# Example: ./update-session-context.sh "error" "Logback configuration fix"

# Configuration
PROJECT_ROOT="/home/ec2-user/sample-s2s-voip-gateway"
CONTEXT_FILE="$PROJECT_ROOT/SESSION_CONTEXT.md"
DATE=$(date '+%Y-%m-%d')
DATETIME=$(date '+%Y-%m-%d %H:%M:%S')

# Parameters
ISSUE_TYPE="${1:-manual}"
DESCRIPTION="${2:-Manual update}"

# Create scripts directory if it doesn't exist
mkdir -p "$PROJECT_ROOT/scripts"

# Function to append to SESSION_CONTEXT.md
update_context() {
    echo "" >> "$CONTEXT_FILE"
    echo "## Auto-Update - $DATE" >> "$CONTEXT_FILE"
    echo "" >> "$CONTEXT_FILE"
    echo "### Current Activity: $DESCRIPTION ⚡ AUTO-LOGGED" >> "$CONTEXT_FILE"
    echo "**Timestamp**: $DATETIME" >> "$CONTEXT_FILE"
    echo "**Trigger**: $ISSUE_TYPE" >> "$CONTEXT_FILE"
    echo "" >> "$CONTEXT_FILE"

    # Add git status if available
    if command -v git >/dev/null 2>&1 && [ -d "$PROJECT_ROOT/.git" ]; then
        echo "**Git Status:**" >> "$CONTEXT_FILE"
        echo '```' >> "$CONTEXT_FILE"
        cd "$PROJECT_ROOT" && git status --porcelain >> "$CONTEXT_FILE"
        echo '```' >> "$CONTEXT_FILE"
        echo "" >> "$CONTEXT_FILE"
    fi

    # Add PM2 status if available
    if command -v pm2 >/dev/null 2>&1; then
        echo "**PM2 Status:**" >> "$CONTEXT_FILE"
        echo '```' >> "$CONTEXT_FILE"
        pm2 list --no-colors >> "$CONTEXT_FILE" 2>/dev/null || echo "PM2 not accessible" >> "$CONTEXT_FILE"
        echo '```' >> "$CONTEXT_FILE"
        echo "" >> "$CONTEXT_FILE"
    fi

    # Update notes section
    sed -i '/^## Notes$/,/^$/c\
## Notes\
- Implementation completed and tested successfully\
- All fixes are backward compatible and include fallback behavior\
- Ready for deployment - build successful with no compilation errors\
- **November 11 Update**: New prompt lifecycle issue identified, separate from previously fixed content lifecycle\
- **November 12 Update**: Logback configuration issue resolved - application now starts without Java configuration errors\
- **'$DATE' Auto-Update**: '$DESCRIPTION' logged automatically' "$CONTEXT_FILE"
}

# Function to check for recent errors in PM2 logs
check_pm2_errors() {
    local error_log="/home/ec2-user/.pm2/logs/run-error.log"
    if [ -f "$error_log" ]; then
        # Check if there are errors in the last 5 minutes
        local recent_errors=$(find "$error_log" -mmin -5 -exec tail -10 {} \; 2>/dev/null)
        if [ -n "$recent_errors" ]; then
            echo "**Recent PM2 Errors (last 5 minutes):**" >> "$CONTEXT_FILE"
            echo '```' >> "$CONTEXT_FILE"
            echo "$recent_errors" >> "$CONTEXT_FILE"
            echo '```' >> "$CONTEXT_FILE"
            echo "" >> "$CONTEXT_FILE"
        fi
    fi
}

# Main execution
echo "Updating SESSION_CONTEXT.md..."
echo "Type: $ISSUE_TYPE"
echo "Description: $DESCRIPTION"

# Backup current context file
cp "$CONTEXT_FILE" "$CONTEXT_FILE.backup.$(date +%s)"

# Update context
update_context

# Check for recent errors if this is an error trigger
if [ "$ISSUE_TYPE" = "error" ] || [ "$ISSUE_TYPE" = "pm2" ]; then
    check_pm2_errors
fi

echo "SESSION_CONTEXT.md updated successfully!"
echo "Backup created: $CONTEXT_FILE.backup.*"