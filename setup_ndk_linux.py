#!/usr/bin/env python3
# encoding=utf-8
import os

if __name__ == '__main__':
    ndk_filepath = 'android-ndk-r20b-linux-x86_64'
    if not os.path.exists(ndk_filepath):
        raise Exception(ndk_filepath + ' does not exist!')

    ndk_filepath = os.path.abspath(ndk_filepath)

    gradle_props_filepath = 'local.properties'

    content = ''

    if os.path.exists(gradle_props_filepath):
        content = open(gradle_props_filepath, mode='rb').read().decode('utf-8')

    if len(content) > 0:
        if content[-1] == '\n':
            content = content + 'ndk.dir=' + ndk_filepath + '\n'
        else:
            content = content + '\nndk.dir=' + ndk_filepath + '\n'
    else:
        content = 'ndk.dir=' + ndk_filepath + '\n'

    open(gradle_props_filepath, mode='wb').write(content.encode('utf-8'))
