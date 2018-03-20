package cop5556sp18;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;


import cop5556sp18.AST.Declaration;

public class SymbolTable {
	
	private int maxScope;
	Stack<Integer> scopeStack;
	
	// key1 - scope, key2 - Ident
	Map<String, Map<Integer, Declaration>> symbolmap;
	
	public SymbolTable() {
		this.maxScope = 0;
		scopeStack = new Stack<Integer>();
		scopeStack.push(0);
		symbolmap = new HashMap<>();
	}
	
	public void enterScope() {
		maxScope++;
		this.scopeStack.push(maxScope);
	}
	
	public void leaveScope() {
		scopeStack.pop();
	}
	
	public int getCurrentScope() {
		return scopeStack.peek();
	}
	
	public boolean insert(String ident, Declaration dec) {
		Map<Integer, Declaration> map;
		
		if(symbolmap.containsKey(ident)) {
			map = symbolmap.get(ident);
		} else {
			map = new HashMap<>();
		}
		
		int currScope = getCurrentScope();
		
		if (map.containsKey(currScope)) {
			return false;
		}
		map.put(currScope, dec);
		symbolmap.put(ident, map);
		return true;
	}
	
	public Declaration lookup(String ident) {
		if (!symbolmap.containsKey(ident)) {
			return null;
		}
		
		Map<Integer, Declaration> map = symbolmap.get(ident);
		
		for (int i = scopeStack.size()-1; i>=0; i--) {
			if (map.containsKey(scopeStack.get(i))) {
				return map.get(scopeStack.get(i));
			}
		}
		
		return null;
	}
	
	@Override
	public String toString() {
		return symbolmap.toString();
	}

	

}
