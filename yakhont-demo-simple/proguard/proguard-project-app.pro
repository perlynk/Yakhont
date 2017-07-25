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

-allowaccessmodification

-dontwarn io.reactivex.**
-dontwarn okhttp3.logging.**

-dontnote rx.schedulers.**

# uncomment if you don't need Location API
# -dontwarn com.google.android.gms.**
# -dontnote com.google.android.gms.**


-keep class akha.yakhont.demosimple.BuildConfig { *; }

-keep class akha.yakhont.demosimple.model.** { *; }

-keep interface akha.yakhont.demosimple.retrofit.** { *; }