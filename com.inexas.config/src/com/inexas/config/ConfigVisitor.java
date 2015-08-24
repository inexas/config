package com.inexas.config;

import java.util.*;
import com.inexas.oak.ast.*;

class ConfigVisitor extends AstVisitor.Base {
	private final Map<String, Object> map = new HashMap<>();

	public Map<String, Object> getMap() {
		return map;
	}

	@Override
	public void enter(ValuePairNode node) {
		final StringBuilder sb = new StringBuilder();
		getPath(node, sb);
		map.put(sb.toString(), node.asObject());
	}

	private void getPath(PairNode node, StringBuilder sb) {
		final PairNode parent = (PairNode)node.getParent();
		if(parent != null) {
			getPath(parent, sb);
		}
		sb.append('/');
		sb.append(node.getName());
	}
}