#!/usr/bin/env bash
# ============================================================================
# skills.sh — JavaFX Skill Set Installer
#
# Installs all 11 JavaFX skills from https://github.com/dongzy0617/javafx-skill
# into the TRAE global skills directory so they are available across all
# projects and sessions.
#
# Skills installed:
#   1.  javafx-requirements      — Requirements engineering
#   2.  javafx-architect         — Architecture design (UML, ADR, DB, STRIDE)
#   3.  javafx-designer          — UI/UX visual design (FXML, CSS, themes)
#   4.  javafx-developer         — Code generation (core skill)
#   5.  javafx-code-reviewer     — Static code review (10 dimensions)
#   6.  javafx-runner            — Runtime verification
#   7.  javafx-tester            — Deep testing (4 parallel tracks)
#   8.  javafx-refactorer        — Code refactoring & tech debt
#   9.  javafx-docgen            — Documentation generation
#   10. javafx-deployer          — Deployment & DevOps
#   11. javafx-orchestrator      — Closed-loop orchestration
#
# Usage:
#   bash skills.sh           # Install / update all skills
#   bash skills.sh --uninstall   # Remove all javafx-* skills
#   bash skills.sh --list        # List installed javafx skills
#   bash skills.sh --help        # Show help
#
# Author: DongZY0617
# License: Apache-2.0
# ============================================================================

set -euo pipefail

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
REPO_URL="https://github.com/dongzy0617/javafx-skill.git"
REPO_NAME="javafx-skill"
SKILLS_DIR_REL="skills"  # Relative path to skills/ inside the repo

SKILLS=(
    "javafx-requirements"
    "javafx-architect"
    "javafx-designer"
    "javafx-developer"
    "javafx-code-reviewer"
    "javafx-runner"
    "javafx-tester"
    "javafx-refactorer"
    "javafx-docgen"
    "javafx-deployer"
    "javafx-orchestrator"
)

# Colors for output
if [[ -t 1 ]]; then
    BOLD='\033[1m'
    GREEN='\033[0;32m'
    YELLOW='\033[0;33m'
    RED='\033[0;31m'
    BLUE='\033[0;34m'
    CYAN='\033[0;36m'
    NC='\033[0m' # No Color
else
    BOLD=''; GREEN=''; YELLOW=''; RED=''; BLUE=''; CYAN=''; NC=''
fi

# ---------------------------------------------------------------------------
# Helper functions
# ---------------------------------------------------------------------------
log_info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }
log_step()  { echo -e "\n${BLUE}${BOLD}==>${NC} ${BOLD}$*${NC}"; }
log_ok()    { echo -e "  ${GREEN}✓${NC} $*"; }
log_fail()  { echo -e "  ${RED}✗${NC} $*"; }

# ---------------------------------------------------------------------------
# Detect TRAE skills directory
# ---------------------------------------------------------------------------
detect_trae_dir() {
    local trae_base=""

    # Try common environment variable overrides first
    if [[ -n "${TRAE_HOME:-}" ]]; then
        trae_base="$TRAE_HOME"
    elif [[ -n "${TRAE_CONFIG_DIR:-}" ]]; then
        trae_base="$TRAE_CONFIG_DIR"
    else
        case "$(uname -s)" in
            MINGW*|MSYS*|CYGWIN*)
                # Windows: use USERPROFILE (more reliable than HOME in Git Bash)
                local userprofile
                userprofile=$(cygpath -u "$USERPROFILE" 2>/dev/null || echo "$HOME")
                # Try .trae-cn first (Chinese edition), then .trae (international)
                if [[ -d "$userprofile/.trae-cn" ]]; then
                    trae_base="$userprofile/.trae-cn"
                elif [[ -d "$userprofile/.trae" ]]; then
                    trae_base="$userprofile/.trae"
                elif [[ -d "$HOME/.trae-cn" ]]; then
                    trae_base="$HOME/.trae-cn"
                elif [[ -d "$HOME/.trae" ]]; then
                    trae_base="$HOME/.trae"
                else
                    # Default to .trae-cn on Windows (this machine's config)
                    trae_base="$userprofile/.trae-cn"
                fi
                ;;
            Darwin)
                # macOS
                if [[ -d "$HOME/.trae-cn" ]]; then
                    trae_base="$HOME/.trae-cn"
                else
                    trae_base="$HOME/.trae"
                fi
                ;;
            Linux)
                if [[ -d "$HOME/.trae-cn" ]]; then
                    trae_base="$HOME/.trae-cn"
                else
                    trae_base="$HOME/.trae"
                fi
                ;;
            *)
                # Fallback
                trae_base="$HOME/.trae"
                ;;
        esac
    fi

    # Build the full skills path
    TRAE_SKILLS_DIR="$trae_base/builtin/global/skills"

    echo -e "  ${CYAN}TRAE config:${NC}  $trae_base"
    echo -e "  ${CYAN}Skills dir:${NC}   $TRAE_SKILLS_DIR"

    # Create directory if it doesn't exist
    if [[ ! -d "$TRAE_SKILLS_DIR" ]]; then
        log_warn "Skills directory does not exist yet. Creating: $TRAE_SKILLS_DIR"
        mkdir -p "$TRAE_SKILLS_DIR"
    fi
}

# ---------------------------------------------------------------------------
# Clone or update the repository
# ---------------------------------------------------------------------------
clone_or_update_repo() {
    local temp_dir="$1"

    log_step "Cloning repository from GitHub"

    if [[ -d "$temp_dir/.git" ]]; then
        log_info "Repository already exists at $temp_dir, pulling latest..."
        git -C "$temp_dir" pull --ff-only --quiet 2>/dev/null || {
            log_warn "Pull failed, trying fresh clone..."
            rm -rf "$temp_dir"
            git clone --depth 1 "$REPO_URL" "$temp_dir" 2>/dev/null
        }
    else
        log_info "Cloning $REPO_URL ..."
        rm -rf "$temp_dir"
        git clone --depth 1 "$REPO_URL" "$temp_dir" 2>/dev/null || {
            log_error "Failed to clone repository. Check your network connection."
            exit 1
        }
    fi

    log_ok "Repository ready"
}

# ---------------------------------------------------------------------------
# Install skills
# ---------------------------------------------------------------------------
install_skills() {
    local repo_dir="$1"

    log_step "Installing ${#SKILLS[@]} skills to TRAE global skills directory"

    local installed=0
    local failed=0
    local skipped=0

    for skill in "${SKILLS[@]}"; do
        local src="$repo_dir/$SKILLS_DIR_REL/$skill"
        local dst="$TRAE_SKILLS_DIR/$skill"

        if [[ ! -d "$src" ]]; then
            log_warn "Skill directory not found in repo: $skill"
            log_fail "$skill (source not found)"
            ((failed++))
            continue
        fi

        if [[ ! -f "$src/SKILL.md" ]]; then
            log_warn "SKILL.md not found for: $skill"
            log_fail "$skill (missing SKILL.md)"
            ((failed++))
            continue
        fi

        # Remove existing installation (if any) for clean update
        if [[ -d "$dst" ]] || [[ -L "$dst" ]]; then
            rm -rf "$dst"
            log_info "  Updating: $skill"
        else
            log_info "  Installing: $skill"
        fi

        # Copy the skill directory
        cp -r "$src" "$dst"

        # Verify
        if [[ -f "$dst/SKILL.md" ]]; then
            log_ok "$skill"
            ((installed++))
        else
            log_fail "$skill (copy verification failed)"
            ((failed++))
        fi
    done

    echo ""
    echo -e "  ${BOLD}Summary:${NC} ${GREEN}$installed installed${NC}, $skipped skipped, $failed failed"

    if [[ $failed -gt 0 ]]; then
        log_error "Some skills failed to install. Check the output above."
        return 1
    fi

    return 0
}

# ---------------------------------------------------------------------------
# Uninstall skills
# ---------------------------------------------------------------------------
uninstall_skills() {
    log_step "Uninstalling JavaFX skills from TRAE"

    local removed=0
    local notfound=0

    for skill in "${SKILLS[@]}"; do
        local dst="$TRAE_SKILLS_DIR/$skill"

        if [[ -d "$dst" ]] || [[ -L "$dst" ]]; then
            rm -rf "$dst"
            log_ok "Removed: $skill"
            ((removed++))
        else
            log_info "  Not installed: $skill"
            ((notfound++))
        fi
    done

    echo ""
    echo -e "  ${BOLD}Summary:${NC} ${GREEN}$removed removed${NC}, $notfound not found"
}

# ---------------------------------------------------------------------------
# List installed skills
# ---------------------------------------------------------------------------
list_skills() {
    log_step "Installed JavaFX skills"

    local found=0
    local missing=0

    printf "  %-28s %-10s %-10s %s\n" "SKILL" "STATUS" "VERSION" "PATH"
    printf "  %-28s %-10s %-10s %s\n" "-----" "------" "-------" "----"

    for skill in "${SKILLS[@]}"; do
        local dst="$TRAE_SKILLS_DIR/$skill"
        local status=""
        local version="-"
        local path="-"

        if [[ -f "$dst/SKILL.md" ]]; then
            status="${GREEN}installed${NC}"
            # Extract version from SKILL.md frontmatter
            version=$(grep -m1 '^  version:' "$dst/SKILL.md" 2>/dev/null | sed 's/.*version: *"*//;s/"*//' || echo "-")
            path="$dst"
            ((found++))
        else
            status="${RED}missing${NC}"
            ((missing++))
        fi

        echo -e "  $(printf '%-28s' "$skill") $(printf '%-19s' "" | sed "s/ / /") $(printf '%-10s' "$version") $path" | sed "s/installed/${GREEN}installed${NC}/;s/missing/${RED}missing${NC}/"

        # Simpler approach:
        if [[ -f "$dst/SKILL.md" ]]; then
            echo -e "  ${GREEN}✓${NC} ${skill%-*}$(echo "$skill" | tail -c +$(( ${#skill} - ${#skill} + ${#skill} )))"
        fi
    done

    # Redo with simpler format
    echo ""
    for skill in "${SKILLS[@]}"; do
        local dst="$TRAE_SKILLS_DIR/$skill"
        if [[ -f "$dst/SKILL.md" ]]; then
            local version
            version=$(grep -m1 'version:' "$dst/SKILL.md" 2>/dev/null | head -1 | sed 's/.*version: *//;s/"//g' || echo "?")
            echo -e "  ${GREEN}✓${NC} $skill (v$version)"
            ((found++))
        else
            echo -e "  ${RED}✗${NC} $skill"
            ((missing++))
        fi
    done

    echo ""
    echo -e "  ${BOLD}Total:${NC} ${GREEN}$found installed${NC}, $missing missing"
}

# ---------------------------------------------------------------------------
# Verify prerequisites
# ---------------------------------------------------------------------------
check_prerequisites() {
    local missing=0

    log_step "Checking prerequisites"

    if ! command -v git &>/dev/null; then
        log_fail "git is not installed"
        ((missing++))
    else
        log_ok "git $(git --version 2>/dev/null | head -1)"
    fi

    if ! command -v cp &>/dev/null; then
        log_fail "cp is not available"
        ((missing++))
    else
        log_ok "cp available"
    fi

    if [[ $missing -gt 0 ]]; then
        log_error "Missing prerequisites. Please install them and try again."
        exit 1
    fi
}

# ---------------------------------------------------------------------------
# Show help
# ---------------------------------------------------------------------------
show_help() {
    cat << 'EOF'
============================================================================
  JavaFX Skill Set Installer
  https://github.com/dongzy0617/javafx-skill
============================================================================

Usage:
  bash skills.sh              Install or update all 11 JavaFX skills
  bash skills.sh --install    Same as above (explicit)
  bash skills.sh --uninstall  Remove all javafx-* skills from TRAE
  bash skills.sh --list       List installed JavaFX skills and versions
  bash skills.sh --help       Show this help message

Environment Variables:
  TRAE_HOME       Override TRAE config directory (default: auto-detect)
  TRAE_CONFIG_DIR Same as TRAE_HOME (alternative name)

Skills (11 total):
  javafx-requirements       Requirements engineering
  javafx-architect          Architecture design (UML, ADR, DB schema, STRIDE)
  javafx-designer           UI/UX visual design (FXML, CSS, themes, icons)
  javafx-developer          Code generation (core skill)
  javafx-code-reviewer      Static code review (10 dimensions)
  javafx-runner             Runtime verification (compile, run, package)
  javafx-tester             Deep testing (4 parallel tracks)
  javafx-refactorer         Code refactoring & technical debt management
  javafx-docgen             Documentation generation (5 doc types)
  javafx-deployer           Deployment & DevOps (CI/CD, signing, rollback)
  javafx-orchestrator       Closed-loop orchestration controller

Supported Platforms:
  - Windows (Git Bash / MSYS2 / Cygwin)
  - macOS
  - Linux

TRAE Editions:
  - .trae-cn  (Chinese edition, auto-detected)
  - .trae     (International edition, auto-detected)

Examples:
  bash skills.sh                  # Standard install
  bash skills.sh --list           # Check what's installed
  bash skills.sh --uninstall      # Clean removal

============================================================================
EOF
}

# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
main() {
    local action="${1:-install}"

    echo ""
    echo -e "${BOLD}${CYAN}"
    echo "  ╔══════════════════════════════════════════════════════════════╗"
    echo "  ║          JavaFX Skill Set Installer  (11 skills)            ║"
    echo "  ║          https://github.com/dongzy0617/javafx-skill         ║"
    echo "  ╚══════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"

    # Parse arguments
    case "$action" in
        --help|-h)
            show_help
            exit 0
            ;;
        --list|-l)
            detect_trae_dir
            list_skills
            exit 0
            ;;
        --uninstall|--remove|-r)
            check_prerequisites
            detect_trae_dir
            uninstall_skills
            echo ""
            log_info "Done. Restart TRAE to apply changes."
            exit 0
            ;;
        --install|-i|"")
            : # proceed to install flow
            ;;
        *)
            log_error "Unknown option: $action"
            echo "Run 'bash skills.sh --help' for usage."
            exit 1
            ;;
    esac

    # Install flow
    check_prerequisites
    detect_trae_dir

    # Use a temp directory for the repo clone
    TEMP_REPO="/tmp/${REPO_NAME}-install-$$"
    if [[ "$(uname -s)" == MINGW* ]] || [[ "$(uname -s)" == MSYS* ]]; then
        # On Windows MSYS2, /tmp maps to a valid temp location
        TEMP_REPO="$HOME/.tmp/${REPO_NAME}-install-$$"
    fi

    # Clean up on exit
    trap 'rm -rf "$TEMP_REPO" 2>/dev/null || true' EXIT

    clone_or_update_repo "$TEMP_REPO"
    install_skills "$TEMP_REPO"

    echo ""
    log_step "Installation complete!"
    echo ""
    echo -e "  ${BOLD}Next steps:${NC}"
    echo -e "  1. ${CYAN}Restart TRAE${NC} (or start a new session) to load the new skills"
    echo -e "  2. Trigger the orchestrator with a prompt like:"
    echo -e "     ${CYAN}\"Orchestrate a full loop for my JavaFX app\"${NC}"
    echo -e "  3. Or use any skill individually, e.g.:"
    echo -e "     ${CYAN}\"Design the architecture for a JavaFX app with SQLite\"${NC}"
    echo ""
    echo -e "  Run ${CYAN}bash skills.sh --list${NC} to verify installation."
    echo ""
}

main "$@"
