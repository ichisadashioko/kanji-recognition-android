#!/usr/bin/env python3
import os
import posixpath
import traceback
import subprocess
from subprocess import PIPE
import urllib.request

from tqdm import tqdm

GOOGLE_JAVA_FORMATTER_URL = 'https://github.com/google/google-java-format/releases/download/google-java-format-1.8/google-java-format-1.8-all-deps.jar'
GOOGLE_JAVA_FORMATTER_JAR_PATH = 'google-java-format-1.8-all-deps.jar'

if not os.path.exists(GOOGLE_JAVA_FORMATTER_JAR_PATH):
    print(f'Downloading Google Java Formatter...')
    urllib.request.urlretrieve(GOOGLE_JAVA_FORMATTER_URL, GOOGLE_JAVA_FORMATTER_JAR_PATH)


def main():
    completed_process = subprocess.run(
        ['git', 'ls-files'],
        stdout=PIPE,
        stderr=PIPE,
    )

    lines = completed_process.stdout.decode('utf-8').split('\n')

    file_list = list(filter(lambda x: len(x) > 0, lines))
    file_list = list(filter(lambda x: os.path.splitext(x)[1] == '.java', file_list))

    pbar = tqdm(file_list)
    for fpath in pbar:
        pbar.set_description(fpath)

        os.system(f'java -jar {GOOGLE_JAVA_FORMATTER_JAR_PATH} -i --aosp {fpath}')


if __name__ == '__main__':
    main()
