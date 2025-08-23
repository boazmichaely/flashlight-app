# Walklight - Google Play Console Upload Guide

## 🚀 Step-by-Step Upload Process

### Step 1: Create App Listing
1. Go to [Google Play Console](https://play.google.com/console)
2. Click **"Create app"**
3. Fill in basic details:
   - **App name**: `Walklight - Safety Flashlight`
   - **Default language**: English (United States)
   - **App or game**: App
   - **Free or paid**: Free
   - **Declarations**: Check content policy compliance

### Step 2: Upload App Bundle (New Account Path)

#### Option A: Internal Testing First (Recommended for new accounts)
1. Go to **"Test and release"** → **"Internal testing"**
2. Click **"Create release"** 
3. Upload `app-release.aab` from PlayStore folder
4. Add yourself as a tester (your email address)
5. Test the app on your device to verify everything works
6. After successful testing, promote to Production (this unlocks production access)

#### Option B: Apply for Production Access
1. Go to **Dashboard** → **"Apply for production access"**
2. Complete additional requirements
3. Wait for approval (may take days)

## 🧪 Internal Testing Process (Detailed)

### After Uploading to Internal Testing:
1. **Add Email**: Add your email as a tester
2. **Review Release**: Click "Review release" → "Start rollout to Internal testing"  
3. **Get Test Link**: You'll receive a Play Store link to install the app
4. **Test on Device**: Install and test all features work correctly
5. **Promote**: Once satisfied, go back to Play Console and "Promote release" → "Production"

### Benefits of Internal Testing First:
- ✅ **Unlocks Production access** automatically
- ✅ **Test real Play Store installation** before public release  
- ✅ **Faster than waiting for production approval**
- ✅ **Can fix any issues** before public launch

### Release Notes to Use:
   ```
   Initial release of Walklight - Smart Safety Flashlight
   
   Features:
   • Dual lighting: LED + screen brightness control
   • Auto-start functionality
   • Intelligent state preservation  
   • Adaptive UI layouts
   • Professional device compatibility
   ```

### Step 3: Store Listing
1. Go to **"Store presence"** → **"Main store listing"**
2. Add content from `store-descriptions.md`:
   - **Short description**: (79 characters)
   - **Full description**: (Copy from file)
   - **App icon**: Upload high-res version of your icon
   - **Feature graphic**: Upload when created (optional)
   - **Screenshots**: Add your app screenshot + icon

### Step 4: Content Rating
1. Go to **"Policy"** → **"App content"** 
2. Complete questionnaire
3. Walklight should be rated **"Everyone"** (no sensitive content)

### Step 5: App Details
1. **Category**: Tools
2. **Tags**: flashlight, LED, safety, walking, torch, emergency
3. **Contact details**: Your email
4. **Privacy policy**: Not required (no personal data collected)

### Step 6: Review and Publish
1. **Review summary** - Check all sections complete
2. **Submit for review** - Google reviews within 24-48 hours
3. **Publishing**: After approval, choose "Release to production"

## 📋 Required Assets Checklist

### ✅ Ready Now
- [x] `app-release.aab` - Signed app bundle
- [x] Store descriptions (short + full)
- [x] App screenshots
- [x] High-res icon (512×512)

### ⏳ Optional/Create Later
- [ ] Feature graphic (1024×500) - Design spec ready
- [ ] Additional screenshots (different devices)
- [ ] Promotional video (can add later)

## ⚠️ Important Notes

### First Upload
- Google review typically takes 24-48 hours
- App will appear in search within hours of approval
- You can update app info anytime after approval

### Future Updates
- Use same keystore file for all updates
- Version code must increase with each update
- Can add feature graphic, more screenshots later

## 🎯 Success Metrics to Track
- Downloads and installs
- User ratings and reviews  
- Search ranking for "flashlight" keywords
- User retention and engagement

**You're ready to go live! 🚀**
