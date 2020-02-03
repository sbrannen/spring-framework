cat tests.txt | grep -v "^#" | sed 's/^/-c=/' | tr '\n' ' ' 
