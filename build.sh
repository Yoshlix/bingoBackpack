#!/bin/bash
# Build script for BingoBackpack with custom Bingo API
set -e

echo "🎯 Building BingoBackpack with custom Bingo API..."
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Step 1: Build Bingo modules
echo -e "${BLUE}📦 Step 1/4: Building Bingo API modules...${NC}"
cd bingo
./gradlew :api:build :common:build :platform:build --quiet
echo -e "${GREEN}✓ Bingo API modules built successfully${NC}"
echo ""

# Step 2: Build Bingo Mod
echo -e "${BLUE}📦 Step 2/4: Building Bingo Mod (MC 1.21.11)...${NC}"
./gradlew :mc1.21.11:build --quiet
cd ..
echo -e "${GREEN}✓ Bingo Mod built successfully${NC}"
echo ""

# Step 3: Build main project
echo -e "${BLUE}📦 Step 3/4: Building BingoBackpack...${NC}"
./gradlew build --quiet
echo -e "${GREEN}✓ BingoBackpack built successfully${NC}"
echo ""

# Step 4: Show output
echo -e "${BLUE}📦 Step 4/4: Copying to output folder...${NC}"
mkdir -p out
cp build/libs/bingobackpack-*.jar out/ 2>/dev/null || true
# Copy only the fat jar (with all dependencies)
cp bingo/mc1.21.11/build/libs/bingo-*-all.jar out/ 2>/dev/null || true
# Remove sources jars and dev jars from output
rm -f out/*-sources.jar out/*-dev.jar 2>/dev/null || true
echo -e "${GREEN}✓ Files copied to out/ folder${NC}"
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
