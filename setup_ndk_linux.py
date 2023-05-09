#!/usr/bin/env python3
# encoding=utf-8
import os

if __name__ == '__main__':
    ndk_filepath = 'android-ndk-r20b'
    if not os.path.exists(ndk_filepath):
        raise Exception(ndk_filepath + ' does not exist!')

    ndk_filepath = os.path.abspath(ndk_filepath)

    # set persistent environment variable
    custom_bashrc_filepath = os.path.abspath('custom_bashrc')
    with open(custom_bashrc_filepath, mode='ab+') as outfile:
        outfile.write(f'\nexport ANDROID_NDK_HOME={ndk_filepath}\n'.encode('utf-8'))
    cmd_str = f'source {custom_bashrc_filepath}'
    print(cmd_str)

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
