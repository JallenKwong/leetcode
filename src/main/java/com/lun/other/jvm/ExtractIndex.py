# -*- coding: utf-8 -*

#提取 # XX # 作索引 [1.XX](#XX), 直接打印出来

import re

chapterName = 'c02'

fileName = chapterName + '\README.md'

textFile = open(fileName, 'r')


counterList = [0] * 6

indexRegex = re.compile(r'^#+ (.*) #+$')

def count(line):
    def coreCount(line, num):
        if line.startswith('#'* num + ' '):
            counterList[num - 1] += 1
            if len(counterList) - num > 0:
                counterList[num:] = [0] * (len(counterList) - num)

    for i in range(2, 7):
        coreCount(line, i)
    
'''    
    if line.startswith('#'* 2 + ' '):
        counterList[1] += 1
        counterList[2:]=[0] * 4

    if line.startswith('#'* 3 + ' '):
        counterList[2] += 1
        counterList[3:]=[0] * 3

    if line.startswith('#'* 4 + ' '):
        counterList[3] += 1
        counterList[4:]=[0] * 2

    if line.startswith('#'* 5 + ' '):
        counterList[4] += 1
        counterList[5:]=[0] * 1

    if line.startswith('#'* 6 + ' '):
        counterList[5] += 1
'''

for line in textFile.readlines():
    if indexRegex.match(line):

        count(line)
        
        mo = indexRegex.search(line)

        # line
        indexName = mo.group(1).decode('utf-8')#decode解码，encode编码

        href = indexName.lower()

        prefix = ''

        for i in range(1, 6):
            if counterList[i] == 0:
                break
            prefix += (str(counterList[i]) + '.')
        
        print '[%s%s](#%s)\n'%(prefix, indexName, href)

#print counterList