package earley;
import java.util.*;

public class EntryTable
{
	public RuleTable ruleTable;
	public ArrayList columns = new ArrayList();

	public HashSet predictedTable = new HashSet();
	public HashMap attachedTable = new HashMap();
	public HashMap attachedByTable  = new HashMap();

	public EntryTable(RuleTable ruleTable)
	{
		this.ruleTable = ruleTable;
	}

	public void addColumn()
	{
		int columnIndex = columns.size();
		Column newColumn = new Column(columnIndex, this);
		columns.add(newColumn);
	}

	public Column getColumn(int columnIndex)
	{
		return (Column) columns.get(columnIndex);
	}

	public int size()
	{
		return columns.size();
	}

	public String toString()
	{
		String output = "";

		for (int curColumnIndex = 0; curColumnIndex < size(); curColumnIndex++)
		{
			output += "COLUMN " + curColumnIndex + "\n" + getColumn(curColumnIndex).toString() + "\n";
		}

		return output;
	}

	public String processSentence(String sentence)
	{
		if (sentence.equals(""))
		{
			return "";
		}

		StringTokenizer st = new StringTokenizer(sentence += " " + "</s>");
		ArrayList words = new ArrayList();

		while (st.hasMoreTokens())
		{
			words.add(st.nextToken());
			addColumn();
		}

		for (int curColumnIndex = 0; curColumnIndex < size(); curColumnIndex++)
		{
			Column curColumn = getColumn(curColumnIndex);
			String word = (String) words.get(curColumnIndex);
			processColumn(curColumn, word);
		}

		return outputParse();
	}

	public void processColumn(Column curColumn, String word)
	{
		if (curColumn.columnIndex == 0)
		{
			predict(curColumn, "ROOT");
		}

		for (int curEntryIndex = 0; curEntryIndex < curColumn.size(); curEntryIndex++)
		{
			Entry curEntry = curColumn.getEntry(curEntryIndex);

			if (curEntry.dotPosition < curEntry.rule.RHS.size())
			{
				String lookingFor = curEntry.afterDot();

				if (ruleTable.isExpandable(lookingFor))
				{
					predict(curColumn, lookingFor);
				}
				else
				{
					scan(curColumn, curEntry, word);
				}
			}
			else
			{
				attach(curColumn, curEntry);
			}
		}
	}

	public void predict(Column curColumn, String LHS)
	{
		String key = curColumn.columnIndex + " " + LHS;

		if (predictedTable.contains(key) == false)
		{
			ListIterator rules = ruleTable.getRules(LHS);

			while (rules.hasNext())
			{
				Rule curRule = (Rule) rules.next();
				Entry newEntry = Entry.predictEntry(curColumn.columnIndex, curRule);
				curColumn.addEntry(newEntry);
			}

			predictedTable.add(key);
		}
	}

	public void scan(Column curColumn, Entry curEntry, String word)
	{
		if (curEntry.afterDot().equals(word))
		{
			int nextColumnIndex = curColumn.columnIndex + 1;
			Column nextColumn = getColumn(nextColumnIndex);

			Entry newEntry = Entry.scanEntry(curEntry);
			nextColumn.addEntry(newEntry);
		}
	}

	public void attach(Column curColumn, Entry curEntry)
	{
		int oldColumnIndex = curEntry.start;
		String LHS = curEntry.rule.LHS;

		if (LHS.equals("ROOT"))
		{
			return;
		}

		attachedByTable.put(curEntry, new LinkedList());
		Column oldColumn = getColumn(oldColumnIndex);

		for (int oldEntryIndex = 0; oldEntryIndex < oldColumn.size(); oldEntryIndex++)
		{
			Entry oldEntry = oldColumn.getEntry(oldEntryIndex);
			Entry customer = Entry.attachEntry(oldEntry, curEntry);
			String key = curColumn.columnIndex + " " + customer.start + " " + customer.rule.toString() + " " + customer.dotPosition;

			if (oldEntry.afterDot().equals(curEntry.rule.LHS))
			{
				if (attachedTable.containsKey(key) == false)
				{
					curColumn.addEntry(customer);
					attachedTable.put(key, customer);

					LinkedList attachees = (LinkedList) attachedByTable.get(curEntry);
					attachees.addLast(customer);
				}
				else
				{
					Entry existingEntry = (Entry) attachedTable.get(key);

					if (customer.weight < existingEntry.weight)
					{
						float reduction = existingEntry.weight - customer.weight;
						updateWeights(existingEntry, reduction);
						existingEntry.replaceWith(customer);
					}
				}
			}
		}
	}

	public void updateWeights(Entry attacher, float reduction)
	{
		if (attachedByTable.containsKey(attacher))
		{
			LinkedList attachees = (LinkedList) attachedByTable.get(attacher);
			ListIterator li = attachees.listIterator();

			while (li.hasNext())
			{
				Entry attachee = (Entry) li.next();
				attachee.weight = attachee.weight - reduction;
			}
		}
	}

	public String outputParse()
	{
		Entry parseStart = findParseStart();

		if (parseStart == null)
		{
			return "\nNONE";
		}

		return "\n" + printEntry(parseStart) + ")\n" + parseStart.weight;
	}

	public Entry findParseStart()
	{
		int lastColumnIndex = size() - 1;
		Column lastColumn = getColumn(lastColumnIndex);

		for (int entryIndex = 0; entryIndex < lastColumn.size(); entryIndex++)
		{
			Entry entry = lastColumn.getEntry(entryIndex);
			int lastPosition = entry.rule.RHS.size();

			if (entry.rule.LHS.equals("ROOT") && entry.dotPosition == lastPosition)
			{
				return entry;
			}
		}

		return null;
	}

	public String printEntry(Entry entry)
	{
		String output = "";

		if (entry.pointer1 == null && entry.pointer2 == null)
		{
			output = "(" + entry.rule.LHS;
		}
		else if (entry.pointer2 == null)
		{
			int beforeDot = entry.dotPosition - 1;
			String terminal = (String) entry.rule.RHS.get(beforeDot);
			output = printEntry(entry.pointer1) + " " + terminal;
		}
		else
		{
			output = printEntry(entry.pointer1) + " " + printEntry(entry.pointer2) + ")";
		}

		return output;
	}
}