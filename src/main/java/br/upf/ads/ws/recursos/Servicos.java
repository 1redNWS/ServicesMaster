package br.upf.ads.ws.recursos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.gson.Gson;

import br.upf.ads.ws.classes.Contagem;
import br.upf.ads.ws.classes.ContainerResposta;
import br.upf.ads.ws.classes.Servico;

@Path("/Servicos")
public class Servicos {

	// CRIAR A CONEXAO COM O BANCO DE DADOS.
	public static Connection conexao;

	static {
		try {

			Class.forName("org.postgresql.Driver");
			conexao = DriverManager.getConnection("jdbc:postgresql://localhost:5432/Trabalho", "postgres", "masterkey");

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// RECURSOS

	// INSERIR -----------------------

	// INSERE SERVIÇO COM JSON PASSADO PELO BODY
	// http://localhost:8080/ServicosREST/api/v1/Servicos
	@POST
	@Consumes(MediaType.APPLICATION_JSON) // consome json pelo body
	public Response inserirServicoByJson(String servicoJson) {

		Servico servico = new Gson().fromJson(servicoJson, Servico.class); // Intancia Servico atraves do JSON recebido.
		return commitServico(servico); // Insere o servico no BD, mas antes valida. Retorna response.
	}

	// INSERE SERVIÇO COM DADOS PASSADOS POR FORM
	// http://localhost:8080/ServicosREST/api/v1/Servicos
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED) // Consome dados de formulario
	public Response inserirServicosByForm(

			@FormParam("numero") Integer numero, @FormParam("descricao") String descricao,
			@FormParam("categoria") String categoria, @FormParam("preco") Double preco,
			@FormParam("localizacao") String localizacao) {

		Servico servico = new Servico(numero, descricao, categoria, preco, localizacao);
		return commitServico(servico); // Insere o servico no BD, mas antes valida. Retorna response.
	}

	// CONSULTAR ----------------------

	// LISTA TODOS OS SERVIÇOS
	// http://localhost:8080/ServicosREST/api/v1/Servicos
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String listaServicos() {
		return searchServico("select * from servico");
	}

	// LISTA OS SERVIÇOS FILTRADOS NA URL
	// http://localhost:8080/ServicosREST/api/v1/Servicos/query?campo=descricao&filtro=alguma
	@GET
	@Path("query")
	@Produces(MediaType.APPLICATION_JSON)
	public Response listaServicoComFiltro(@QueryParam("campo") String campo, @QueryParam("filtro") String filtro) {
		String rowCount = null;
		Status status = null;

		if (campo == null) {
			status = Status.BAD_REQUEST;
			rowCount = "{'mensagem': 'O query parameter campo não foi enviado'}";
		} else if (filtro == null) {
			status = Status.BAD_REQUEST;
			rowCount = "{'mensagem': 'O query parameter filtro não foi enviado'}";
		} else if (!campo.equals("numero") && !campo.equals("descricao") && !campo.equals("categoria")
				&& !campo.equals("preco") && !campo.equals("localizacao")) {
			status = Status.BAD_REQUEST;
			rowCount = "{'mensagem': 'O query parameter campo deve conter um destes"
					+ "cinco valores: numero, descricao, categoria, preco e descricao'}";
		} else {
			status = Status.OK;
			if (campo.equals("numero")) {
				rowCount = searchServico("select * from servico where numero = " + filtro);
			} else if (campo.equals("preco")) {
				rowCount = searchServico("select * from servico where preco = " + filtro);
			} else {
				rowCount = searchServico("select * from servico where " + campo + " like '%" + filtro + "%'");
			}
		}

		return Response.status(status).type(MediaType.APPLICATION_JSON).entity(rowCount).build();
	}

	// LISTA SERVIÇO POR NÚMERO PASSADO NA URL
	// http://localhost:8080/ServicosREST/api/v1/Servicos/numero
	@GET
	@Path("{numero}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response listaByPathParam(@PathParam("numero") String numero) {
		String rowCount = searchServico("Select * from servico where numero = " + numero);
		Status status = Status.BAD_REQUEST;

		if (!rowCount.isEmpty()) {
			status = Status.OK;
		}

		return Response.status(status).type(MediaType.APPLICATION_JSON).entity(rowCount).build();
	}

	// LISTA SERVIÇOS COM O MAIOR PREÇO
	// http://localhost:8080/ServicosREST/api/v1/Servicos/maiorpreco
	@GET
	@Path("maiorpreco")
	@Produces(MediaType.APPLICATION_JSON)
	public String listaServicoMaiorPreco() {
		return searchServico("SELECT * FROM servico WHERE preco = (SELECT max(preco) FROM servico);");
	}

	// LISTA SERVIÇOS COM O MENOR PREÇO
	// http://localhost:8080/ServicosREST/api/v1/Servicos/menorpreco
	@GET
	@Path("menorpreco")
	@Produces(MediaType.APPLICATION_JSON)
	public String listaServicoMenorPreco() {
		return searchServico("SELECT * FROM servico WHERE preco = (SELECT min(preco) FROM servico);");
	}

	// RETORNA O NÚMERO TOTAL DE SERVIÇOS
	// http://localhost:8080/ServicosREST/api/v1/Servicos/totalServicos
	@GET
	@Path("totalServicos")
	@Produces(MediaType.TEXT_PLAIN)
	public String totalDeServicos() {
		return searchNumero("SELECT count(id) as contagem FROM servico;");
	}

	// LISTA O NÚMERO DE SERVIÇOS POR CATEGORIA
	// http://localhost:8080/ServicosREST/api/v1/Servicos/servicosCategoria
	@GET
	@Path("servicosCategoria")
	@Produces(MediaType.APPLICATION_JSON)
	public String servicosCategoria() {
		return searchContagens("SELECT categoria as nome, count(id) as numero from servico GROUP BY categoria;");
	}

	// LISTA O NÚMERO DE SERVIÇOS POR LOCALIZÇÃO
	// http://localhost:8080/ServicosREST/api/v1/Servicos/servicosLocalizacao
	@GET
	@Path("servicosLocalizacao")
	@Produces(MediaType.APPLICATION_JSON)
	public String sericosLocalizacao() {
		return searchContagens("SELECT localizacao as nome, count(id) as numero from servico GROUP BY localizacao;");
	}

	// DELETAR ------------------------

	// DELETA TODOS OS SERVIÇOS
	// http://localhost:8080/ServicosREST/api/v1/Servicos
	@DELETE
	public Response deleteAll() {
		Status status = Status.BAD_REQUEST;

		ContainerResposta cr = new ContainerResposta();
		cr.error = true;
		cr.message = "NENHUM SERVIÇO ENCONTRADO !";

		int rowCount = executeSQL("DELETE FROM servico;");
		if (rowCount > 0) {
			status = Status.OK;
			cr.error = false;
			cr.message = "REMOVIDO(S)" + rowCount + " SERVIÇOS(s)!";
		}

		return Response.status(status).type(MediaType.APPLICATION_JSON).entity(new Gson().toJson(cr)).build();
	}

	// DELETA OS SERVIÇOS COM NÚMERO PASSADO POR PARÂMETRO NA URL
	// http://localhost:8080/ServicosREST/api/v1/Servicos/numero
	@DELETE
	@Path("{numero}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteByPathParam(@PathParam("numero") String numero) {

		Status status = Status.BAD_REQUEST;

		ContainerResposta cr = new ContainerResposta();
		cr.error = true;
		cr.message = "NENHUM SERVIÇO ENCONTRADO COM O NÚMERO " + numero + "!";

		int rowCount = executeSQL("DELETE FROM servico WHERE numero = " + numero);
		if (rowCount > 0) {
			status = Status.OK;
			cr.error = false;
			cr.message = "SERVIÇO DE NÚMERO " + numero + " REMOVIDO!";
		}

		return Response.status(status).type(MediaType.APPLICATION_JSON).entity(new Gson().toJson(cr)).build();
	}

	// DELETA OS SERVIÇOS COM FILTRO PASSADO POR PARÂMETRO NA URL
	// http://localhost:8080/ServicosREST/api/v1/Servicos/query?campo=descricao&filtro=alguma
	@DELETE
	@Path("query")
	public Response deleteComFiltro(@QueryParam("campo") String campo, @QueryParam("filtro") String filtro) {
		int rowCount = 0;
		Status status = null;

		ContainerResposta cr = new ContainerResposta();
		cr.error = true;
		cr.message = "NENHUM SERVIÇO ENCONTRADO COM O CAMPO '" + campo + "' DE FILTRO '" + filtro + "'";

		if (campo == null) {
			status = Status.BAD_REQUEST;
			cr.message = "mensagem': 'O query parameter campo não foi enviado.";
		} else if (filtro == null) {
			status = Status.BAD_REQUEST;
			cr.message = "O query parameter filtro não foi enviado.";
		} else if (!campo.equals("numero") && !campo.equals("descricao") && !campo.equals("categoria")
				&& !campo.equals("preco") && !campo.equals("localizacao")) {
			status = Status.BAD_REQUEST;
			cr.message = "mensagem': 'O query parameter campo deve conter um destes"
					+ "cinco valores: numero, descricao, categoria, preco e descricao";
		} else {

			if (campo.equals("numero")) {
				rowCount = executeSQL("Delete from servico where numero = " + filtro);
			} else if (campo.equals("preco")) {
				rowCount = executeSQL("Delete from servico where preco = " + filtro);
			} else {
				rowCount = executeSQL("Delete from servico where " + campo + " like '%" + filtro + "%'");
			}
		}
		if (rowCount > 0) {
			status = Status.OK;
			cr.error = false;
			cr.message = "FOI (FORAM) REMOVIDO(S) " + rowCount + " SERVIÇO(S) COM O CAMPO '" + campo + "' DE FILTRO '"
					+ filtro + "'";
		}

		return Response.status(status).type(MediaType.APPLICATION_JSON).entity(new Gson().toJson(cr)).build();
	}

	// ALTERAR

	// ALTERA SERVIÇO COM JSON PASSADO PELO BODY
	// http://localhost:8080/ServicosREST/api/v1/Servicos
	@PUT
	@Consumes(MediaType.APPLICATION_JSON) // consome json pelo body
	public Response alterarServicoByJson(String servicoJson) {

		Servico servico = new Gson().fromJson(servicoJson, Servico.class); // Intancia Servico atraves do JSON recebido.
		return updateServico(servico); // Insere o servico no BD, mas antes valida. Retorna response.
	}

	// ALTERA SERVIÇO COM DADOS PASSADOS POR FORM
	// http://localhost:8080/ServicosREST/api/v1/Servicos
	@PUT
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED) // Consome dados de formulario
	public Response alterarServicosByForm(

			@FormParam("numero") Integer numero, @FormParam("descricao") String descricao,
			@FormParam("categoria") String categoria, @FormParam("preco") Double preco,
			@FormParam("localizacao") String localizacao) {

		Servico servico = new Servico(numero, descricao, categoria, preco, localizacao);
		return updateServico(servico); // Insere o servico no BD, mas antes valida. Retorna response.
	}

	// Outros Métodos

	// Método que executa sql passado por parâmetro.
	private int executeSQL(String sql) {
		int rowCount = 0;
		try {
			Statement st;
			st = conexao.createStatement();
			rowCount = st.executeUpdate(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return rowCount;

	}

	// Método de consulta ao DB e retorna um array de produtos em JSON.
	public String searchServico(String sql) {

		Statement st;
		List<Servico> servicos = new ArrayList<Servico>();

		try {
			st = conexao.createStatement();
			ResultSet result = st.executeQuery(sql);

			while (result.next()) {
				Servico servico = new Servico();
				servico.setNumero(result.getInt("numero"));
				servico.setDescricao(result.getString("descricao"));
				servico.setCategoria(result.getString("categoria"));
				servico.setPreco(result.getDouble("preco"));
				servico.setLocalizacao(result.getString("localizacao"));

				servicos.add(servico);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new Gson().toJson(servicos);
	}

	// Valida o produto e insere no BD
	private Response commitServico(Servico servico) {
		Response.Status status = null;
		ContainerResposta retorno = new ContainerResposta();
		retorno.error = true;

		String search = searchNumero("SELECT count(id) as contagem FROM servico WHERE numero = " + servico.getNumero());
		if (Integer.parseInt(search) != 0) {
			status = Status.BAD_REQUEST;
			retorno.message = "Número de serviço INVÁLIDO - já cadastrado no Banco de Dados!";
		} else {

			if (servico.getNumero() == null) {

				status = Status.BAD_REQUEST;
				retorno.message = "Número do Serviço INVÁLIDO";

			} else if (servico.getDescricao() == null || servico.getDescricao().isEmpty()) {

				status = Status.BAD_REQUEST;
				retorno.message = "Descrição do Serviço INVÁLIDA";

			} else if (servico.getCategoria() == null || servico.getCategoria().isEmpty()) {

				status = Status.BAD_REQUEST;
				retorno.message = "Categoria do Serviço INVÁLIDA";

			} else if (servico.getPreco() == null || servico.getPreco() < 0) {

				status = Status.BAD_REQUEST;
				retorno.message = "Preço do Serviço INVÁLIDO";
			} else if (servico.getLocalizacao() == null || servico.getLocalizacao().isEmpty()) {

				status = Status.BAD_REQUEST;
				retorno.message = "Localização do Serviço INVÁLIDA";

			} else {

				// Inserir servico no banco de dados;
				try {
					PreparedStatement pst = conexao.prepareStatement(
							"INSERT INTO servico (numero, descricao, categoria, preco, localizacao) VALUES (?, ?, ?, ?, ?);");
					pst.setInt(1, servico.getNumero());
					pst.setString(2, servico.getDescricao());
					pst.setString(3, servico.getCategoria());
					pst.setDouble(4, servico.getPreco());
					pst.setString(5, servico.getLocalizacao());
					int rowCount = pst.executeUpdate(); // executa o pst e retorna o número de linhas afetadas.

					if (rowCount > 0) {
						retorno.error = false;
						retorno.servico = servico;
						status = Response.Status.CREATED; // 201 - criado
					} else {
						retorno.error = true;
						status = Response.Status.NOT_MODIFIED; // 304 - não modificou nada
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		// design pattern Builder
		return Response.status(status).type(MediaType.APPLICATION_JSON).entity(new Gson().toJson(retorno)).build();
	}

	// Valida o produto e insere no BD
	private Response updateServico(Servico servico) {
		Response.Status status = null;
		ContainerResposta retorno = new ContainerResposta();
		retorno.error = true;

		if (servico.getNumero() == null) {

			status = Status.BAD_REQUEST;
			retorno.message = "Número do Serviço INVÁLIDO";

		} else if (servico.getDescricao() == null || servico.getDescricao().isEmpty()) {

			status = Status.BAD_REQUEST;
			retorno.message = "Descrição do Serviço INVÁLIDA";

		} else if (servico.getCategoria() == null || servico.getCategoria().isEmpty()) {

			status = Status.BAD_REQUEST;
			retorno.message = "Categoria do Serviço INVÁLIDA";

		} else if (servico.getPreco() == null || servico.getPreco() < 0) {

			status = Status.BAD_REQUEST;
			retorno.message = "Preço do Serviço INVÁLIDO";
		} else if (servico.getLocalizacao() == null || servico.getLocalizacao().isEmpty()) {

			status = Status.BAD_REQUEST;
			retorno.message = "Localização do Serviço INVÁLIDA";

		} else {

			// Inserir servico no banco de dados;
			try {
				PreparedStatement pst = conexao.prepareStatement(
						"UPDATE servico SET descricao = ?, categoria = ?, preco = ?, localizacao = ? WHERE numero = ?;");
				pst.setString(1, servico.getDescricao());
				pst.setString(2, servico.getCategoria());
				pst.setDouble(3, servico.getPreco());
				pst.setString(4, servico.getLocalizacao());
				pst.setInt(5, servico.getNumero());
				int rowCount = pst.executeUpdate(); // executa o pst e retorna o número de linhas afetadas.

				if (rowCount > 0) {
					retorno.error = false;
					retorno.servico = servico;
					status = Response.Status.CREATED; // 201 - criado
				} else {
					retorno.error = true;
					status = Response.Status.NOT_MODIFIED; // 304 - não modificou nada
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// design pattern Builder
		return Response.status(status).type(MediaType.APPLICATION_JSON).entity(new Gson().toJson(retorno)).build();
	}

	// Método de consulta ao DB e retorna um array de contagens em JSON.
	private String searchContagens(String sql) {
		Statement st;
		List<Contagem> contagens = new ArrayList<Contagem>();

		try {
			st = conexao.createStatement();
			ResultSet result = st.executeQuery(sql);

			while (result.next()) {
				Contagem contagem = new Contagem();
				contagem.setNome(result.getString("nome"));
				contagem.setNumero(result.getInt("numero"));
				contagens.add(contagem);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new Gson().toJson(contagens);
	}

	// Método de consulta ao DB e retorna um número de contagem em JSON.
	private String searchNumero(String sql) {
		Statement st;
		String retorno = "";

		try {
			st = conexao.createStatement();
			ResultSet result = st.executeQuery(sql);

			while (result.next()) {
				retorno = result.getString("contagem");
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return retorno;
	}

}
