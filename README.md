# AndroidTestMockPermissionUtils

This is a workaround implementation that is intended to solve the issue of running test cases which require permissions to be granted at runtime and also (wait for it) .... REVOKED! 😁

Yes I'm serious as it took me a while to figure it out but finally got it working.

##### OK Let's Talk Serious, Show Me The Facts

Several cases have showed that revoking a permission while running tests will result crashes such as [here](https://stackoverflow.com/q/43462172/2949966):
```
Test failed to run to completion. Reason: 'Instrumentation run failed due to 'Process crashed.''.
```

This is reasonable as Android would need a way to signal the app that a permission has been revoked. And since there aren't any ways of signaling the app while it is running or closed, Android would force close based on the fact that the moment the app is opened again or restarted it'll check the permissions and act appropriately when they're denied.

Since we can't request the Android System to revoke the permissions while the app is running tests, the only other way is to fake the results when the app is checking the permissions. It occurred to me that there if we can fake the results even if the app had the permissions granted, we can test the permission denied scenarios without the app ever realizing that the results are mocked or fake, in the end it's not going to need the permission within these flows.

##### The Digging

While digging in the implementation of methods like ```checkSelfPermissions```, it turned out that the implementation is handled by the ```Context``` class, and since we can wrap the original ```Context``` with a custom ```ContextWrapper``` called ```CustomBuildContext```, it made sense to implement a ```CustomBuildBaseActivity``` class that manipulates the ```Context``` when the ```attachBaseContext``` is called and wrap it with ```CustomBuildContext``` which has extra logic of mocking the results of checking the permissions. ```CustomBuildBaseActivity``` is then extended by all activities in the app to incorporate the extra logic and now that the app will calls for checking permissions will be directed to the ```CustomBuildContext```, tests can mock the results to test scenarios when the permissions are denied.

The term ```Build``` in the names of the classes here is simply to state that these classes are dependent on the build variant (debug or release). Since our tests usually run on the default variant debug (more info [here](https://developer.android.com/studio/test/index.html#change_the_test_build_type)), the implementations of these classes would contain the extra logic to support the needs during testing in the debug variant only, while in the release variant these classes would be completely empty so not to have extra unnecessary logic that might affect the app in production.

##### More Digging

Revoking the permissions before the tests start seemed like a nice idea, but wasn't enough. Our tests needed to check and ensure that the app behaves correctly when it not only checks for these permissions, but also when it requests for them too. Hence spent more digging and found our savior which is the ```ActivityCompat```. It is called whenever ```requestSelfPermissions``` or similar is called in an ```Activity``` or a ```Fragment```. While looking into it's implementation, it turned out that it checks for a certain variable of type ```ActivityCompat.PermissionsCompatDelegate``` before it sends the request to the lower level implementations of the Android System. If the variable was set, it'll handle the request for the permissions and return the response instead of the Android System doing it. The interface class was meant for use with the Instant Apps as stated in the [release notes](https://developer.android.com/topic/libraries/support-library/revisions.html#27-0-0), however one could implement the class and set it in the ```ActivityCompat``` with ```ActivityCompat.setPermissionCompatDelegate()``` hence override the default implementation and manipulate the flow. This made possible for the tests to also mock the results and the actual flow of requesting permissions from the Android System, and hence a custom implementation has been made to do just that.

##### Tying Things Up

Finally, the implementation comes down to the following 3 classes used to manage and simulate the results of Android Runtime Permissions' checks and requests:
 - ```PermissionRule``` - This class is the main starting point for tests. It is a JUnit ```TestRule``` which could define the state of the permissions before the test starts. For tests that require the permissions to be granted, it runs a shell command with the shell user privileges to grant the permission hence this is using the typical implementation from the Android System. If the tests require the permission to be denied, regardless of whether the Android System has the permission granted or not to the app, the other two classes are notified by the ```PermissionRule``` class and the checks and requests within the app will be mocked. The ```PermissionRule``` class has the ```PermissionCompatDelegate``` implemented to receive calls from the app requesting to grant permissions and hence it will mock the flow and grant the permissions without having the Android System to show a user dialog for the request. Furthermore, the ```PermissionRule``` has a method called ```isRequestPermissionCalled()``` which could be used in asserts in the tests to ensure that the app has requested the permissions.
 - ```CustomBuildBaseActivity``` - This is the custom ```Activity``` with the extra logic of attaching or injecting the ```CustomBuildContext``` into the ```Activity``` and any underlying ```Fragments```. Meaning when calling ```getActivity```, ```getContext```, ```MainActivity.this``` or similar, the ```CustomBuildContext``` will be returned. Furthermore it overrides the method ```shouldShowRequestPermissionRationale``` in order to mock it results and contains the variables (```HashedMap```s) which are set by ```PermissionRule``` to mock the results and flow.
 - ```CustomBuildContext``` - This is the ```ContextWrapper``` with the ```checkSelfPermission()``` and similar methods overridden to return the mocked result from ```CustomBuildBaseActivity``` and hence apps would be calling it's implementation at runtime.

##### Let's Test

We talked a lot about our testing, testing this is necessary 😏. An instrumented test has been implemented called ```PermissionsInstrumentedTest``` to exhibit the implementations. The app has a button which when clicked shows a dialog with the list of files and folders in the sdcard, hence the ```READ_EXTERNAL_STORAGE``` is needed. The test enforces the rules of running the test on Marshmallow or above device, granting the permission and then revoking the permission, and finally opening the app before starting a test. In case you'd like to clone the repo and run the code, first run the app **without** running the tests, hence granting the permission to the app on the "SHOW SD CARD" button. Check the logs as they should be similar to below:
```
...
03-09 02:13:17.918 I/CustomBuildBaseActivity: attachBaseContext: called
03-09 02:13:17.919 I/CustomBuildContext: CustomBuildContext: base: android.app.ContextImpl@23ddd20
03-09 02:13:17.919 I/CustomBuildContext: CustomBuildContext: called
...
03-09 02:13:18.992 I/CustomBuildContext: checkPermission: called
03-09 02:13:18.992 I/MainActivityFragment: onCreate: permission denied
03-09 02:13:19.011 I/CustomBuildContext: checkPermission: called
03-09 02:13:19.011 I/MainActivity: onCreate: permission denied
...
03-09 02:13:44.941 I/MainActivityFragment: onClick: called
03-09 02:13:44.941 I/CustomBuildContext: checkPermission: called
03-09 02:13:47.746 I/MainActivityFragment: onRequestPermissionsResult: called
03-09 02:13:47.746 I/MainActivityFragment: onRequestPermissionsResult: permission granted
...
```
Then run tests in ```PermissionsInstrumentedTest``` and check the logs again (note that the execution order of tests is random, so block of logs could be in different places):
 - ```activityPermissionDeniedTest()```:
```
...
03-09 02:46:50.188 I/TestRunner: started: activityPermissionDeniedTest(com.ahasbini.test.permission_utils.PermissionsInstrumentedTest)
...
03-09 02:46:50.193 I/ActivityTestRule: Launching activity: ComponentInfo{com.ahasbini.test.permission_utils/com.ahasbini.test.permission_utils.MainActivity}
03-09 02:46:50.195 D/MonitoringInstr: execStartActivity(context, ibinder, ibinder, activity, intent, int, bundle
03-09 02:46:50.212 I/CustomBuildBaseActivity: attachBaseContext: called
03-09 02:46:50.212 I/CustomBuildContext: CustomBuildContext: base: android.app.ContextImpl@97d28b1
03-09 02:46:50.213 I/CustomBuildContext: CustomBuildContext: called
...
03-09 02:46:50.223 I/CustomBuildContext: checkPermission: called
03-09 02:46:50.223 I/CustomBuildContext: checkPermission: found permission: android.permission.READ_EXTERNAL_STORAGE
03-09 02:46:50.223 I/CustomBuildContext: checkPermission: result: -1
03-09 02:46:50.223 I/MainActivityFragment: onCreate: permission denied
03-09 02:46:50.224 I/CustomBuildContext: checkPermission: called
03-09 02:46:50.224 I/CustomBuildContext: checkPermission: found permission: android.permission.READ_EXTERNAL_STORAGE
03-09 02:46:50.224 I/CustomBuildContext: checkPermission: result: -1
03-09 02:46:50.224 I/MainActivity: onCreate: permission denied
...
03-09 02:46:50.506 I/PermissionsInstrumentedTest: activityPermissionDeniedTest: starting
03-09 02:46:50.506 I/PermissionsInstrumentedTest: activityPermissionDeniedTest: getting activity and testing
03-09 02:46:50.506 I/CustomBuildContext: checkSelfPermission: called
03-09 02:46:50.506 I/CustomBuildContext: checkSelfPermission: found permission: android.permission.READ_EXTERNAL_STORAGE
03-09 02:46:50.506 I/CustomBuildContext: checkSelfPermission: result: -1
03-09 02:46:50.506 I/PermissionsInstrumentedTest: activityPermissionDeniedTest: finished
...
03-09 02:46:50.662 I/TestRunner: finished: activityPermissionDeniedTest(com.ahasbini.test.permission_utils.PermissionsInstrumentedTest)
...
```
 - ```targetContextPermissionGrantedTest()```:
```
...
03-09 02:46:50.830 I/TestRunner: started: targetContextPermissionGrantedTest(com.ahasbini.test.permission_utils.PermissionsInstrumentedTest)
...
03-09 02:46:50.836 I/ActivityTestRule: Launching activity: ComponentInfo{com.ahasbini.test.permission_utils/com.ahasbini.test.permission_utils.MainActivity}
03-09 02:46:50.853 D/MonitoringInstr: execStartActivity(context, ibinder, ibinder, activity, intent, int, bundle
03-09 02:46:50.871 I/CustomBuildBaseActivity: attachBaseContext: called
03-09 02:46:50.871 I/CustomBuildContext: CustomBuildContext: base: android.app.ContextImpl@49c9b15
03-09 02:46:50.871 I/CustomBuildContext: CustomBuildContext: called
...
03-09 02:46:50.885 I/CustomBuildContext: checkPermission: called
03-09 02:46:50.886 I/CustomBuildContext: checkPermission: found permission: android.permission.READ_EXTERNAL_STORAGE
03-09 02:46:50.886 I/CustomBuildContext: checkPermission: result: -1
03-09 02:46:50.886 I/MainActivityFragment: onCreate: permission denied
03-09 02:46:50.892 I/CustomBuildContext: checkPermission: called
03-09 02:46:50.892 I/CustomBuildContext: checkPermission: found permission: android.permission.READ_EXTERNAL_STORAGE
03-09 02:46:50.893 I/CustomBuildContext: checkPermission: result: -1
03-09 02:46:50.893 I/MainActivity: onCreate: permission denied
...
03-09 02:46:51.110 I/PermissionsInstrumentedTest: targetContextPermissionGrantedTest: started
03-09 02:46:51.110 I/PermissionsInstrumentedTest: targetContextPermissionGrantedTest: getting target context from instrumentation
03-09 02:46:51.111 I/PermissionsInstrumentedTest: targetContextPermissionGrantedTest: finished
...
03-09 02:46:51.307 I/TestRunner: finished: targetContextPermissionGrantedTest(com.ahasbini.test.permission_utils.PermissionsInstrumentedTest)
...
```
 - ```permissionCompatDelegateTest()```:
```
...
03-09 02:46:48.567 I/TestRunner: started: permissionCompatDelegateTest(com.ahasbini.test.permission_utils.PermissionsInstrumentedTest)
...
03-09 02:46:48.572 I/ActivityTestRule: Launching activity: ComponentInfo{com.ahasbini.test.permission_utils/com.ahasbini.test.permission_utils.MainActivity}
03-09 02:46:48.573 D/MonitoringInstr: execStartActivity(context, ibinder, ibinder, activity, intent, int, bundle
03-09 02:46:48.590 I/CustomBuildBaseActivity: attachBaseContext: called
03-09 02:46:48.591 I/CustomBuildContext: CustomBuildContext: base: android.app.ContextImpl@cc6f85a
03-09 02:46:48.591 I/CustomBuildContext: CustomBuildContext: called
...
03-09 02:46:48.661 I/CustomBuildContext: checkPermission: called
03-09 02:46:48.661 I/CustomBuildContext: checkPermission: found permission: android.permission.READ_EXTERNAL_STORAGE
03-09 02:46:48.661 I/CustomBuildContext: checkPermission: result: -1
03-09 02:46:48.662 I/MainActivityFragment: onCreate: permission denied
03-09 02:46:48.671 I/CustomBuildContext: checkPermission: called
03-09 02:46:48.671 I/CustomBuildContext: checkPermission: found permission: android.permission.READ_EXTERNAL_STORAGE
03-09 02:46:48.671 I/CustomBuildContext: checkPermission: result: -1
03-09 02:46:48.671 I/MainActivity: onCreate: permission denied
...
03-09 02:46:49.031 I/PermissionsInstrumentedTest: permissionCompatDelegateTest: starting
03-09 02:46:49.031 I/PermissionsInstrumentedTest: permissionCompatDelegateTest: clicking show sd card button
03-09 02:46:49.041 D/InputManagerEventInjectionStrategy: Creating injection strategy with input manager.
03-09 02:46:49.057 I/ViewInteraction: Checking 'MatchesViewAssertion{viewMatcher=is displayed on the screen to the user}' assertion on view with string from resource id: <2131558440>[show_sd_card] value: Show SD Card
03-09 02:46:49.069 I/ViewInteraction: Performing 'single click' action on view with string from resource id: <2131558440>[show_sd_card] value: Show SD Card
03-09 02:46:49.118 I/MainActivityFragment: onClick: called
03-09 02:46:49.118 I/CustomBuildContext: checkPermission: called
03-09 02:46:49.119 I/CustomBuildContext: checkPermission: found permission: android.permission.READ_EXTERNAL_STORAGE
03-09 02:46:49.119 I/CustomBuildContext: checkPermission: result: -1
03-09 02:46:49.119 I/PermissionCompatDelegate: requestPermissions: called
03-09 02:46:49.126 I/PermissionCompatDelegate: run: granting the permission android.permission.READ_EXTERNAL_STORAGE
03-09 02:46:49.144 I/MainActivityFragment: onRequestPermissionsResult: called
03-09 02:46:49.144 I/MainActivityFragment: onRequestPermissionsResult: permission granted
...
03-09 02:46:49.399 I/PermissionsInstrumentedTest: permissionCompatDelegateTest: requestPermissions asserted true
03-09 02:46:49.399 I/PermissionsInstrumentedTest: permissionCompatDelegateTest: clicking ok button
03-09 02:46:49.402 I/ViewInteraction: Checking 'MatchesViewAssertion{viewMatcher=is displayed on the screen to the user}' assertion on view with string from resource id: <17039370>[ok] value: OK
03-09 02:46:49.411 I/ViewInteraction: Performing 'single click' action on view with string from resource id: <17039370>[ok] value: OK
...
03-09 02:46:49.788 I/PermissionsInstrumentedTest: permissionCompatDelegateTest: finished
...
03-09 02:46:49.866 I/TestRunner: finished: permissionCompatDelegateTest(com.ahasbini.test.permission_utils.PermissionsInstrumentedTest)
...
```
The first two tests ```activityPermissionDeniedTest()``` and ```targetContextPermissionGrantedTest()``` are more of unit tests, while the last one ```permissionCompatDelegateTest()``` is an instrumented test. When running the tests, the app has already been granted the permission.

In the ```activityPermissionDeniedTest()``` (between the logs ```activityPermissionDeniedTest: starting``` and ```activityPermissionDeniedTest: finished```), the test grabbed the ```Activity``` and called ```checkSelfPermissions()``` on it expecting the result to be ```PERMISSION_DENIED```. What happened is the ```CustomBuildContext``` was called and has returned with mocked result ```PERMISSION_DENIED``` hence succeeding the test.

In the ```targetContextPermissionGrantedTest()``` (between the logs ```targetContextPermissionGrantedTest: starting``` and ```targetContextPermissionGrantedTest: finished```), the test grabbed the application context from the instrumentation registry using ```InstrumentationRegistry.getTargetContext()``` and called ```checkSelfPermissions()``` on it expecting the result to be ```PERMISSION_GRANTED```. The ```CustomBuildContext``` implementations were not called and hence the result of the check was returned by the default implementations from the Android System which is ```PERMISSION_GRANTED``` since our app has already been granted by our previous use. This displays that the target or app ```Context``` retrieved from ```InstrumentationRegistry``` is not affected by the implementations and should be cautious when to use it. Since this is a normal behavior from the Android Support Test Libraries to have an app context that is created when starting the tests, this can be considered as a successful test.

In the ```permissionCompatDelegateTest()``` (between the logs ```permissionCompatDelegateTest: starting``` and ```permissionCompatDelegateTest: finished```), a complete UI test is done using Espresso which checks how the app behaves when the permission is granted, which is a dialog should automatically appear just as we've seen by our use before. Hence the app will call ```requestPermissions()```, the ```PermissionCompatDelegate``` implementation will handle the request, ensure that the permissions are granted and notify the app back with the granted results, hence simulating the flow of user granting the permissions without having the Android System Dialog for requesting the permission to appear. Furthermore the test asserts that the app has called ```requestPermissions()``` and finally clicks the "OK" button on the dialog that's listing the list of folders and files in the SDCard ending up with a successful test.

##### The Final Result

Definitely this is a huge step forward in improving app quality with more extensive and automated testing. The only criticisms that I can think of is that rather than having code that is contained in the test packages only, we have some extra codes that are part of the app. Meaning if there was some way that the context and the methods be overridden while creating the ```Activity``` within the test framework it would've been better than having the ```Custom``` classes being extended and injected. Unfortunately while I was digging I ended up not finding any easy way of doing this within the Android Support Test Libraries as it seemed as if there was a complicated way of creating the ```Activity``` and ```Context``` while the test runner ```AndroidJUnitRunner``` is starting. Also the other downside is this depends on Android Support Libraries v27.0.0. As far as I've checked, the implementation is compatible with the latest release v27.1.0. I'm hoping that future releases don't break the compatibility in order for us to benefit from it.

##### The Future

Of course this has a lot of room for improvement. Some of the things I have in mind are:
 - make this into an SDK for easier integration
 - provide more assertions
 - test this more

If you guys feel like you have the stuff to contribute, please feel free to create issues and pull requests as they will be most definitely welcomed and reviewed when I have the time. We're all looking for a common goal which is to build better apps and this will definitely help us to do so.

##### Bonus

On several occasions, I've seen several posts, libraries, and plugins that would turn off the animations before the tests start. The idea is animations needs to be turned off before starting the tests in order for Espresso based (or other frameworks) assertions and checks to be executed successfully since animations would halt the test until the UI is static or stable to validate the assertion. This needs the ```SET_ANIMATION_SCALE``` permission to be granted at runtime across all Android versions, even if device API is pre-Marshmallow. However most of the implementations I've seen would be to add a command line or gradle script to be executed before executing the tests command/task, or use Espresso's ```GrantPermissionRule``` which only works on Marshmallow and above devices. But as far as I've checked, I did not find an all round and efficient solution to turn off the animations before the tests start, turn them back on after the tests finish, without having to write a script in an external tool or build system and that covers all Android versions. Hence I created an implementation which does just that using the implemented ```PermissionsRule```. The ```BaseTest``` which is extended by all tests contains the ```PermissionRule``` to grant the permission for ```SET_ANIMATION_SCALE``` and disable the animations before the tests start and enable them back after the tests finish. It runs the shell grant command even on devices that are pre-Marshmallow to automate this pre-requisite. Of course the rule and pre-requisites are done once before and after all tess using ```ClassRule```, ```BeforeClass``` and ```AfterClass```.
