#
#  Copyright (C) 2015-2017 akha, a.k.a. Alexander Kharitonov
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.


-keep class akha.yakhont.callback.lifecycle.BaseActivityLifecycleProceed$ActivityLifecycle
-keep class akha.yakhont.callback.lifecycle.BaseFragmentLifecycleProceed$FragmentLifecycle

-keep class akha.yakhont.support.callback.lifecycle.BaseActivityLifecycleProceed$ActivityLifecycle
-keep class akha.yakhont.support.callback.lifecycle.BaseFragmentLifecycleProceed$FragmentLifecycle

-dontnote akha.yakhont.callback.lifecycle.BaseActivityLifecycleProceed$ActivityLifecycle
-dontnote akha.yakhont.callback.lifecycle.BaseFragmentLifecycleProceed$FragmentLifecycle

-dontnote akha.yakhont.support.callback.lifecycle.BaseActivityLifecycleProceed$ActivityLifecycle
-dontnote akha.yakhont.support.callback.lifecycle.BaseFragmentLifecycleProceed$FragmentLifecycle

-keep class * extends akha.yakhont.callback.BaseCallbacks$BaseActivityCallbacks {
    public void onActivity*(...);
}

-keep class * extends akha.yakhont.callback.BaseCallbacks$BaseFragmentCallbacks {
    public void onFragment*(...);
}

-keep class * extends akha.yakhont.support.callback.BaseCallbacks$BaseActivityCallbacks {
    public void onActivity*(...);
}

-keep class * extends akha.yakhont.support.callback.BaseCallbacks$BaseFragmentCallbacks {
    public void onFragment*(...);
}

-dontnote akha.yakhont.callback.BaseCallbacks$BaseActivityCallbacks
-dontnote akha.yakhont.callback.BaseCallbacks$BaseFragmentCallbacks

-dontnote akha.yakhont.support.callback.BaseCallbacks$BaseActivityCallbacks
-dontnote akha.yakhont.support.callback.BaseCallbacks$BaseFragmentCallbacks
