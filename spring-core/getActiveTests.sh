cat tests.txt | grep -v "^#" | grep -v "^$" | grep -v ".+\.Abstract.+Tests$" | sed 's/^/-c=/' | tr '\n' ' ' 
