#!/usr/bin/env bash
# Fetches recent merged PRs and default-branch commits from a list of OSS
# WebView-based Android browsers and asks Claude to summarize additions
# and improvements in 繁體中文 (zh-tw), so that EinkBro can pick up ideas.
#
# Output: tools/reports/scout-YYYY-MM-DD.md (also tee'd to stdout).

set -eo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
LIST_FILE_DEFAULT="$SCRIPT_DIR/webview_browsers.txt"
REPORTS_DIR="$SCRIPT_DIR/reports"
DEFAULT_DAYS=30

usage() {
  cat <<'EOF'
Usage: tools/scout-browsers.sh [options]

Options:
  --days N            Window size in days (default: 30).
  --since YYYY-MM-DD  Explicit start date; overrides --days.
  --repo OWNER/NAME   Restrict to one repo (repeatable). Skips list file.
  --no-summarize      Collect raw data only; skip the claude call.
  --list FILE         Repo list file (default: tools/webview_browsers.txt).
  -h, --help          Show this help.
EOF
}

DAYS=$DEFAULT_DAYS
SINCE=""
SUMMARIZE=1
LIST_FILE="$LIST_FILE_DEFAULT"
REPOS=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    --days) DAYS="$2"; shift 2 ;;
    --since) SINCE="$2"; shift 2 ;;
    --repo) REPOS+=("$2"); shift 2 ;;
    --no-summarize) SUMMARIZE=0; shift ;;
    --list) LIST_FILE="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "unknown arg: $1" >&2; usage >&2; exit 2 ;;
  esac
done

if [[ -z "$SINCE" ]]; then
  if date -v-1d +%Y-%m-%d >/dev/null 2>&1; then
    SINCE=$(date -v-"${DAYS}"d +%Y-%m-%d)
  else
    SINCE=$(date -d "$DAYS days ago" +%Y-%m-%d)
  fi
fi

need() { command -v "$1" >/dev/null 2>&1 || { echo "missing dependency: $1" >&2; exit 3; }; }
need gh
need jq
if [[ $SUMMARIZE -eq 1 ]]; then need claude; fi
gh auth status >/dev/null 2>&1 || { echo "gh is not authenticated; run: gh auth login" >&2; exit 4; }

if [[ ${#REPOS[@]} -eq 0 ]]; then
  [[ -f "$LIST_FILE" ]] || { echo "list file not found: $LIST_FILE" >&2; exit 5; }
  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line%%#*}"
    line="$(echo "$line" | tr -d '[:space:]')"
    [[ -z "$line" ]] && continue
    REPOS+=("$line")
  done < "$LIST_FILE"
fi

if [[ ${#REPOS[@]} -eq 0 ]]; then
  echo "no repos to scan" >&2
  exit 6
fi

mkdir -p "$REPORTS_DIR"
TODAY=$(date +%Y-%m-%d)
NOW=$(date +"%Y-%m-%d %H:%M:%S %Z")
REPORT="$REPORTS_DIR/scout-$TODAY.md"
RAW_TMP=$(mktemp)
PR_TITLES_TMP=$(mktemp)
trap 'rm -f "$RAW_TMP" "$PR_TITLES_TMP"' EXIT

fetch_repo() {
  local repo="$1"
  local raw_file="$2"

  printf '\n### %s\n\n' "$repo" >> "$raw_file"

  local pr_json pr_err
  if ! pr_json=$(gh pr list --repo "$repo" --state merged \
        --search "merged:>=$SINCE" \
        --json number,title,body,mergedAt,author,url \
        --limit 200 2>/tmp/scout_err); then
    pr_err=$(cat /tmp/scout_err)
    printf -- '- ⚠ data unavailable: %s\n' "$pr_err" >> "$raw_file"
    echo "[$repo] PR fetch failed: $pr_err" >&2
    return
  fi

  : > "$PR_TITLES_TMP"
  echo "$pr_json" | jq -r '.[] | .title | sub("[[:space:]]+$"; "")' >> "$PR_TITLES_TMP"

  echo "$pr_json" | jq -r '
    .[] |
    "- PR #\(.number): \(.title)  (\(.mergedAt | split("T")[0]), @\(.author.login // "unknown"))"
    + (
        if (.body // "" | length) > 0 then
          "\n  " + ((.body | gsub("\r"; "") | gsub("\n"; " ") | gsub("[ ]+"; " ") | .[0:240]))
          + (if (.body | length) > 240 then "…" else "" end)
        else "" end
      )
  ' >> "$raw_file"

  local commits_json
  if ! commits_json=$(gh api "repos/$repo/commits?since=${SINCE}T00:00:00Z&per_page=100" --paginate 2>/tmp/scout_err); then
    pr_err=$(cat /tmp/scout_err)
    printf -- '- ⚠ commits unavailable: %s\n' "$pr_err" >> "$raw_file"
    echo "[$repo] commit fetch failed: $pr_err" >&2
    return
  fi

  echo "$commits_json" | jq -r '
    [ .[] | select((.parents | length) < 2) | {
        sha: .sha[0:7],
        msg: (.commit.message | split("\n")[0]),
        author: (.commit.author.name // .author.login // "unknown")
      } ] | .[] | "\(.sha)\t\(.msg)\t\(.author)"
  ' | while IFS=$'\t' read -r sha msg author; do
    [[ -z "$sha" ]] && continue
    # Strip trailing " (#1234)" appended by squash-merge before dedup comparison.
    stripped="$(printf '%s' "$msg" | sed -E 's/[[:space:]]*\(#[0-9]+\)[[:space:]]*$//')"
    if grep -Fxq -- "$stripped" "$PR_TITLES_TMP"; then
      continue
    fi
    printf -- '- %s %s  (@%s)\n' "$sha" "$msg" "$author" >> "$raw_file"
  done
}

echo "Scouting ${#REPOS[@]} repos since $SINCE..." >&2
for repo in "${REPOS[@]}"; do
  echo "  → $repo" >&2
  fetch_repo "$repo" "$RAW_TMP"
done

PROMPT_HEADER=$(cat <<'EOF'
你是一個資深的 Android 瀏覽器開發者助理。下方是多個開源 WebView-based 瀏覽器專案最近合併的 PR 與 default branch 上的 commits 清單。請以繁體中文整理：

1. 每個專案以 `### owner/repo` 為小節標題，逐一條列「新增功能」與「改進」。略過純粹的依賴升級、CI 設定、文件修正、字串翻譯、測試、code style。如果該專案沒有實質功能變更，寫「（無顯著功能變動）」。
2. 最後加一個 `## 共同趨勢` 段落，列出在兩個或以上專案出現的主題；這些是最值得 EinkBro 評估移植的候選。

只輸出 markdown，不要前言或結語。資料如下：

EOF
)

SUMMARY=""
if [[ $SUMMARIZE -eq 1 ]]; then
  echo "Asking Claude to summarize..." >&2
  CLAUDE_INPUT=$(mktemp)
  {
    printf '%s\n' "$PROMPT_HEADER"
    cat "$RAW_TMP"
  } > "$CLAUDE_INPUT"

  if ! SUMMARY=$(claude -p "$(cat "$CLAUDE_INPUT")" 2>/tmp/scout_err); then
    echo "claude failed: $(cat /tmp/scout_err)" >&2
    echo "falling back to raw-only report; rerun manually with: claude -p \"\$(cat $CLAUDE_INPUT)\"" >&2
    SUMMARIZE=0
  else
    rm -f "$CLAUDE_INPUT"
  fi
fi

{
  echo "# WebView Browser Activity — since $SINCE"
  echo
  echo "Generated: $NOW · Window: $DAYS days · Repos: ${#REPOS[@]}"
  echo
  if [[ $SUMMARIZE -eq 1 ]]; then
    echo "## Summary (zh-tw)"
    echo
    echo "$SUMMARY"
    echo
  fi
  echo "## Raw data"
  cat "$RAW_TMP"
} > "$REPORT"

cat "$REPORT"
echo >&2
echo "Report written to: $REPORT" >&2
