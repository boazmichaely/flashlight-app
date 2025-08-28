## Reliable Preference List Dividers (Material)

Symptoms addressed:
- Dividers not rendering on some OEMs when relying solely on default Preference dividers.

Recipe
1) Use `PreferenceFragmentCompat` and set an explicit divider drawable and height:
   - `setDivider(ContextCompat.getDrawable(requireContext(), R.drawable.divider_preference))`
   - `setDividerHeight(1)`

2) Add `MaterialDividerItemDecoration` to the RecyclerView to reinforce separators:
```java
RecyclerView rv = getListView();
if (rv != null) {
  while (rv.getItemDecorationCount() > 0) rv.removeItemDecorationAt(0);
  MaterialDividerItemDecoration dec = new MaterialDividerItemDecoration(requireContext(), RecyclerView.VERTICAL);
  dec.setDividerColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
  dec.setDividerThickness(1);
  rv.addItemDecoration(dec);
}
```

3) On each row in `res/xml/settings_preferences.xml`, use divider hints where needed:
   - `app:allowDividerAbove="true"`
   - `app:allowDividerBelow="true"`
   - `android:selectable="false"` on non-interactive rows to avoid ripple feedback.

4) Provide a simple divider drawable `res/drawable/divider_preference.xml`:
```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
  <size android:height="1dp"/>
  <solid android:color="@android:color/darker_gray"/>
</shape>
```

5) Verify on device:
- Open side sheet → list shows thin visible dividers between rows across OEM variants.


