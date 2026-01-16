#!/bin/bash
# Build script for BingoBackpack with custom Bingo API
set -e

echo "🎯 Building BingoBackpack with custom Bingo API..."
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Step 1: Build Bingo modules
echo -e "${BLUE}📦 Step 1/5: Building Bingo API modules...${NC}"
cd bingo
./gradlew :api:build :common:build :platform:build --quiet
echo -e "${GREEN}✓ Bingo API modules built successfully${NC}"
echo ""

# Step 2: Build Bingo Mod
echo -e "${BLUE}📦 Step 2/5: Building Bingo Mod (MC 1.21.11)...${NC}"
./gradlew :mc1.21.11:build --quiet
cd ..
echo -e "${GREEN}✓ Bingo Mod built successfully${NC}"
echo ""

# Step 3: Build main project
echo -e "${BLUE}📦 Step 3/5: Building BingoBackpack...${NC}"
./gradlew build --quiet
echo -e "${GREEN}✓ BingoBackpack built successfully${NC}"
echo ""

# Step 4: Copy to output folder
echo -e "${BLUE}📦 Step 4/5: Copying to output folder...${NC}"
mkdir -p out
cp build/libs/bingobackpack-*.jar out/ 2>/dev/null || true
# Copy the normal jar (not fat jar - it has fabric dependencies properly declared)
cp bingo/mc1.21.11/build/libs/bingo-*+mc1.21.11.jar out/ 2>/dev/null || true
# Remove sources jars, dev jars, and fat jars from output
rm -f out/*-sources.jar out/*-dev.jar out/*-all.jar 2>/dev/null || true
echo -e "${GREEN}✓ Files copied to out/ folder${NC}"
echo ""

# Step 5: Setup run/mods for development
echo -e "${BLUE}📦 Step 5/5: Setting up run/mods for development...${NC}"
mkdir -p run/mods
rm -f run/mods/bingo-*.jar 2>/dev/null || true
cp bingo/mc1.21.11/build/libs/bingo-*+mc1.21.11.jar run/mods/ 2>/dev/null || true
rm -f run/mods/*-sources.jar run/mods/*-all.jar 2>/dev/null || true
echo -e "${GREEN}✓ run/mods ready for development${NC}"
echo ""

echo "========================================"
echo -e "${GREEN}🎉 Build complete!${NC}"
echo "========================================"
echo ""
echo "Output files in out/:"
ls -la out/*.jar 2>/dev/null || echo "  (no files found)"
echo ""
echo -e "${BLUE}📋 Installation:${NC}"
echo "   Copy BOTH JAR files from out/ to your server's mods folder."
echo ""
echo -e "${YELLOW}⚡ Development:${NC}"
echo "   Run: ./gradlew runClient"
echo ""
