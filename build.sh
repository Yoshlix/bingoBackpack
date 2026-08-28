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
echo -e "${BLUE}📦 Step 1/6: Building Bingo API modules...${NC}"
cd bingo
./gradlew :api:build :common:build :platform:build --quiet
echo -e "${GREEN}✓ Bingo API modules built successfully${NC}"
echo ""

# Step 2: Build Bingo Mod
echo -e "${BLUE}📦 Step 2/6: Building Bingo Mod (MC 26.2)...${NC}"
./gradlew :mc26.2:build --quiet
cd ..
echo -e "${GREEN}✓ Bingo Mod built successfully${NC}"
echo ""

# Step 3: Build main project
echo -e "${BLUE}📦 Step 3/6: Building BingoBackpack...${NC}"
./gradlew build --quiet
echo -e "${GREEN}✓ BingoBackpack built successfully${NC}"
echo ""

# Step 4: Build Discord Service
echo -e "${BLUE}📦 Step 4/6: Building Discord Service...${NC}"
cd discord-service
./gradlew shadowJar --quiet
cd ..
echo -e "${GREEN}✓ Discord Service built successfully${NC}"
echo ""

# Step 5: Copy to output folder
echo -e "${BLUE}📦 Step 5/6: Copying to output folder...${NC}"
mkdir -p out
cp build/libs/bingobackpack-*.jar out/ 2>/dev/null || true
# Copy the normal jar (not fat jar - it has fabric dependencies properly declared)
cp bingo/mc26.2/build/libs/bingo-*+mc26.2.jar out/ 2>/dev/null || true
# Copy the Discord Service fat jar
cp discord-service/build/libs/discord-service-*-all.jar out/ 2>/dev/null || true
# Remove sources jars, dev jars from output (but keep -all.jar for discord-service)
rm -f out/*-sources.jar out/*-dev.jar 2>/dev/null || true
rm -f out/bingo-*-all.jar 2>/dev/null || true
echo -e "${GREEN}✓ Files copied to out/ folder${NC}"
echo ""

# Step 6: Setup run/mods for development
echo -e "${BLUE}📦 Step 6/6: Setting up run/mods for development...${NC}"
mkdir -p run/mods
rm -f run/mods/bingo-*.jar 2>/dev/null || true
cp bingo/mc26.2/build/libs/bingo-*+mc26.2.jar run/mods/ 2>/dev/null || true
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
echo "   1. Copy BOTH mod JAR files to your server's mods folder:"
echo "      - bingobackpack-*.jar"
echo "      - bingo-*.jar"
echo "   2. Run discord-service-*-all.jar separately with Java 21+"
echo ""
echo -e "${YELLOW}⚡ Development:${NC}"
echo "   Run: ./gradlew runClient"
echo ""
