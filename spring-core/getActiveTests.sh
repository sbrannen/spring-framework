cat tests.txt | grep -v "^#" | grep -v "^$" | sed 's/^/-c=/' | tr '\n' ' ' 
